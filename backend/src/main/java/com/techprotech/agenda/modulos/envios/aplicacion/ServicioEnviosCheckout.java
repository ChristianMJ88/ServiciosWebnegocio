package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.envios.api.dto.EnvioPedidoResponse;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EmpresaEnvioConfigEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioPedidoEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.SucursalEnvioOrigenEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EmpresaEnvioConfigRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EnvioPedidoRepositorio;
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

    public ServicioEnviosCheckout(EnvioPedidoRepositorio envios, CheckoutOrdenRepositorio ordenes,
                                  CheckoutOrdenDetalleRepositorio detalles,
                                  EmpresaEnvioConfigRepositorio configuraciones,
                                  SucursalEnvioOrigenRepositorio origenes,
                                  ProductoRepositorio productos) {
        this.envios = envios;
        this.ordenes = ordenes;
        this.detalles = detalles;
        this.configuraciones = configuraciones;
        this.origenes = origenes;
        this.productos = productos;
    }

    @Transactional
    public void preparar(CheckoutOrdenEntidad orden) {
        if (!"PAGADA".equals(orden.getEstado()) || !"ENVIO_NACIONAL".equals(orden.getModalidadEntrega())) return;
        EmpresaEnvioConfigEntidad config = configuraciones.findById(orden.getEmpresaId()).orElse(null);
        EnvioPedidoEntidad envio = envios.findByEmpresaIdAndCheckoutOrdenId(orden.getEmpresaId(), orden.getId())
                .orElseGet(EnvioPedidoEntidad::new);
        if (envio.getEstado() != null
                && Set.of("GUIA_GENERADA", "EN_TRANSITO", "ENTREGADO", "CANCELADO").contains(envio.getEstado())) return;
        envio.setEmpresaId(orden.getEmpresaId());
        envio.setCheckoutOrdenId(orden.getId());
        envio.setSucursalId(orden.getSucursalId());
        envio.setProveedor(config == null ? "ENVIA" : config.getProveedor());
        envio.setAmbiente(config == null ? "PRUEBA" : config.getAmbiente());
        envio.setCostoCliente(orden.getCostoEnvio() == null ? BigDecimal.ZERO : orden.getCostoEnvio());
        envio.setMoneda(orden.getMoneda());

        Bloqueo bloqueo = evaluar(orden, config);
        envio.setEstado(bloqueo.estado());
        envio.setMotivoBloqueo(bloqueo.motivo());
        envios.save(envio);
    }

    @Transactional(readOnly = true)
    public List<EnvioPedidoResponse> listar(Long empresaId) {
        return envios.findTop20ByEmpresaIdOrderByCreadoEnDesc(empresaId).stream().map(this::respuesta).toList();
    }

    private Bloqueo evaluar(CheckoutOrdenEntidad orden, EmpresaEnvioConfigEntidad config) {
        if (config == null || !config.isHabilitado()) {
            return new Bloqueo("BLOQUEADO_CONFIGURACION", "Activa la integración de Envia.com para este negocio");
        }
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
        return new Bloqueo("REQUIERE_COTIZACION",
                "Falta seleccionar y guardar una tarifa de Envia.com antes de generar la guía");
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
        return new EnvioPedidoResponse(envio.getId(), orden.getPublicId(), envio.getEstado(), envio.getMotivoBloqueo(),
                envio.getAmbiente(), envio.getPaqueteria(), envio.getServicio(), envio.getNumeroGuia(),
                envio.getUrlEtiqueta(), envio.getEstadoRastreo(), envio.getCostoProveedor(), envio.getCostoCliente(),
                orden.getTotal(), envio.getMoneda(), orden.getNombreDestinatario(), destino,
                envio.getCreadoEn(), envio.getActualizadoEn());
    }

    private List<String> textos(String... valores) {
        return Arrays.stream(valores).filter(valor -> valor != null && !valor.isBlank()).distinct().toList();
    }

    private record Bloqueo(String estado, String motivo) {}
}
