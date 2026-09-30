package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.envios.api.dto.EnvioPedidoResponse;
import com.techprotech.agenda.modulos.envios.api.dto.ActualizarEnvioManualRequest;
import com.techprotech.agenda.modulos.envios.api.dto.EnvioEventoResponse;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EmpresaEnvioConfigEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioPedidoEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.SucursalEnvioOrigenEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EmpresaEnvioConfigRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EnvioPedidoRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EnvioCotizacionRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EnvioEventoRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.SucursalEnvioOrigenRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServicioEnviosCheckout {
    private final EnvioPedidoRepositorio envios;
    private final CheckoutOrdenRepositorio ordenes;
    private final CheckoutOrdenDetalleRepositorio detalles;
    private final EmpresaEnvioConfigRepositorio configuraciones;
    private final SucursalEnvioOrigenRepositorio origenes;
    private final ProductoRepositorio productos;
    private final EnvioCotizacionRepositorio cotizaciones;
    private final EnvioEventoRepositorio eventos;
    private final ServicioTrazabilidadEnvio trazabilidad;

    public ServicioEnviosCheckout(EnvioPedidoRepositorio envios, CheckoutOrdenRepositorio ordenes,
                                  CheckoutOrdenDetalleRepositorio detalles,
                                  EmpresaEnvioConfigRepositorio configuraciones,
                                  SucursalEnvioOrigenRepositorio origenes,
                                  ProductoRepositorio productos,
                                  EnvioCotizacionRepositorio cotizaciones,
                                  EnvioEventoRepositorio eventos,
                                  ServicioTrazabilidadEnvio trazabilidad) {
        this.envios = envios;
        this.ordenes = ordenes;
        this.detalles = detalles;
        this.configuraciones = configuraciones;
        this.origenes = origenes;
        this.productos = productos;
        this.cotizaciones = cotizaciones;
        this.eventos = eventos;
        this.trazabilidad = trazabilidad;
    }

    @Transactional
    public void preparar(CheckoutOrdenEntidad orden) {
        if (!"PAGADA".equals(orden.getEstado()) || !"ENVIO_NACIONAL".equals(orden.getModalidadEntrega())) return;
        EmpresaEnvioConfigEntidad config = configuraciones.findById(orden.getEmpresaId()).orElse(null);
        EnvioPedidoEntidad envio = envios.findByEmpresaIdAndCheckoutOrdenId(orden.getEmpresaId(), orden.getId())
                .orElseGet(EnvioPedidoEntidad::new);
        if (envio.getEstado() != null
                && Set.of("LISTO_PARA_GUIA", "GENERANDO_GUIA", "GUIA_GENERADA", "EN_TRANSITO", "ENTREGADO",
                "INCIDENCIA", "ERROR_GUIA", "CANCELADO").contains(envio.getEstado())) return;
        if ("MANUAL".equals(envio.getProveedor()) && "PENDIENTE_GESTION_MANUAL".equals(envio.getEstado())) return;
        envio.setEmpresaId(orden.getEmpresaId());
        envio.setCheckoutOrdenId(orden.getId());
        envio.setSucursalId(orden.getSucursalId());
        envio.setEnvioCotizacionId(orden.getEnvioCotizacionId());
        boolean integrado = config != null && config.isHabilitado()
                && orden.getEnvioCotizacionId() != null && !orden.getEnvioCotizacionId().isBlank();
        envio.setProveedor(integrado ? config.getProveedor() : "MANUAL");
        envio.setAmbiente(integrado ? config.getAmbiente() : "NO_APLICA");
        envio.setCostoCliente(orden.getCostoEnvio() == null ? BigDecimal.ZERO : orden.getCostoEnvio());
        envio.setMoneda(orden.getMoneda());
        if (orden.getEnvioCotizacionId() != null) {
            cotizaciones.findByIdAndEmpresaId(orden.getEnvioCotizacionId(), orden.getEmpresaId()).ifPresent(cotizacion -> {
                envio.setPaqueteria(cotizacion.getPaqueteria());
                envio.setServicio(cotizacion.getServicio());
                envio.setCostoProveedor(cotizacion.getCostoProveedor());
            });
        }

        Bloqueo bloqueo = integrado
                ? evaluar(orden, config)
                : new Bloqueo("PENDIENTE_GESTION_MANUAL",
                "Pago confirmado. Registra la paquetería y la guía cuando contrates el envío");
        envio.setEstado(bloqueo.estado());
        envio.setMotivoBloqueo(bloqueo.motivo());
        boolean nuevo = envio.getId() == null;
        EnvioPedidoEntidad guardado = envios.save(envio);
        if (nuevo) trazabilidad.registrar(guardado, "PREPARACION", bloqueo.motivo(), null);
    }

    @Transactional(readOnly = true)
    public List<EnvioPedidoResponse> listar(Long empresaId) {
        return envios.findTop20ByEmpresaIdOrderByCreadoEnDesc(empresaId).stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public EnvioPedidoResponse obtener(Long empresaId, Long envioId) {
        EnvioPedidoEntidad envio = envios.findById(envioId)
                .filter(item -> empresaId.equals(item.getEmpresaId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Envío no encontrado"));
        return respuesta(envio);
    }

    @Transactional
    public EnvioPedidoResponse actualizarManual(Long empresaId, Long envioId, ActualizarEnvioManualRequest request) {
        EnvioPedidoEntidad envio = envios.findById(envioId)
                .filter(item -> empresaId.equals(item.getEmpresaId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Envío no encontrado"));
        if (Set.of("ENTREGADO", "CANCELADO").contains(envio.getEstado())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "El envío ya no admite cambios operativos");
        }
        envio.setProveedor("MANUAL");
        envio.setPaqueteria(request.paqueteria().trim());
        envio.setServicio(texto(request.servicio()));
        envio.setNumeroGuia(request.numeroGuia().trim());
        envio.setUrlEtiqueta(texto(request.urlEtiqueta()));
        envio.setUrlRastreo(texto(request.urlEtiqueta()));
        envio.setCostoProveedor(request.costoProveedor().setScale(2, java.math.RoundingMode.HALF_UP));
        envio.setEstado(request.estado());
        envio.setEstadoRastreo(request.estado());
        envio.setMotivoBloqueo(null);
        envio.setUltimoError(null);
        envio = envios.save(envio);
        trazabilidad.registrar(envio, "GESTION_MANUAL", "El negocio actualizó la guía externa", null);
        return respuesta(envio);
    }

    private Bloqueo evaluar(CheckoutOrdenEntidad orden, EmpresaEnvioConfigEntidad config) {
        if ("CUENTA_PROPIA".equals(config.getModoCredencial())
                && (config.getTokenCifrado() == null || config.getTokenCifrado().isBlank())) {
            return new Bloqueo("BLOQUEADO_CREDENCIAL", "Configura una credencial de Envia.com");
        }
        Optional<SucursalEnvioOrigenEntidad> origen = origenes.findByEmpresaIdOrderBySucursalIdAsc(orden.getEmpresaId())
                .stream().filter(item -> item.isActivo() && orden.getSucursalId().equals(item.getSucursalId())).findFirst();
        if (origen.isEmpty()) {
            return new Bloqueo("BLOQUEADO_ORIGEN", "Configura un origen activo para la sucursal de la venta");
        }
        List<CheckoutOrdenDetalleEntidad> lineas = detalles
                .findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(orden.getEmpresaId(), orden.getId());
        Set<Long> ids = lineas.stream().map(CheckoutOrdenDetalleEntidad::getProductoId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ProductoEntidad> catalogo = productos.findByEmpresaIdAndIdIn(orden.getEmpresaId(), ids).stream()
                .collect(Collectors.toMap(ProductoEntidad::getId, item -> item));
        List<String> incompletos = ids.stream().map(catalogo::get).filter(this::sinDimensiones)
                .map(item -> item == null ? "Producto no disponible" : item.getNombre()).sorted().toList();
        if (!incompletos.isEmpty()) {
            return new Bloqueo("BLOQUEADO_DIMENSIONES",
                    "Completa peso y medidas: " + String.join(", ", incompletos));
        }
        return new Bloqueo("LISTO_PARA_GUIA", "La tarifa está seleccionada y el pedido puede generar su guía");
    }

    private boolean sinDimensiones(ProductoEntidad producto) {
        return producto == null || noPositivo(producto.getPesoKg()) || noPositivo(producto.getLargoCm())
                || noPositivo(producto.getAnchoCm()) || noPositivo(producto.getAltoCm());
    }

    private boolean noPositivo(BigDecimal valor) { return valor == null || valor.signum() <= 0; }

    private EnvioPedidoResponse respuesta(EnvioPedidoEntidad envio) {
        CheckoutOrdenEntidad orden = ordenes.findById(envio.getCheckoutOrdenId()).orElseThrow();
        String destino = orden.getDireccionFormateada() == null || orden.getDireccionFormateada().isBlank()
                ? String.join(", ", textos(orden.getCalle(), orden.getNumeroExterior(), orden.getColonia(),
                orden.getMunicipio(), orden.getEstadoDireccion(), orden.getCodigoPostal()))
                : orden.getDireccionFormateada();
        List<EnvioEventoResponse> historial = eventos
                .findByEmpresaIdAndEnvioPedidoIdOrderByCreadoEnAscIdAsc(envio.getEmpresaId(), envio.getId()).stream()
                .map(item -> new EnvioEventoResponse(item.getId(), item.getTipo(), item.getEstado(),
                        item.getDescripcion(), item.getCreadoEn())).toList();
        return new EnvioPedidoResponse(envio.getId(), orden.getPublicId(), envio.getEstado(), envio.getMotivoBloqueo(),
                envio.getAmbiente(), envio.getPaqueteria(), envio.getServicio(), envio.getNumeroGuia(),
                envio.getUrlEtiqueta(), envio.getUrlRastreo(), envio.getEstadoRastreo(), envio.getCostoProveedor(), envio.getCostoCliente(),
                orden.getTotal(), envio.getMoneda(), orden.getNombreDestinatario(), destino,
                envio.getIntentos(), envio.getUltimoError(), historial,
                envio.getCreadoEn(), envio.getActualizadoEn());
    }

    private List<String> textos(String... valores) {
        return Arrays.stream(valores).filter(valor -> valor != null && !valor.isBlank()).distinct().toList();
    }

    private String texto(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

    private record Bloqueo(String estado, String motivo) {}
}
