package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoEnvios;

import com.techprotech.agenda.modulos.envios.dominio.ProveedorGuiasEnvio;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioGuiasEnvio {
    private final java.time.Clock reloj;
    private final EnvioPedidoRepositorio envios;
    private final CheckoutOrdenRepositorio ordenes;
    private final CheckoutOrdenDetalleRepositorio detalles;
    private final EmpresaEnvioConfigRepositorio configuraciones;
    private final SucursalEnvioOrigenRepositorio origenes;
    private final ProductoRepositorio productos;
    private final ResolutorCredencialEnvio credenciales;
    private final ServicioTrazabilidadEnvio trazabilidad;
    private final ServicioOutboxCorreoEnvios notificaciones;
    private final Map<String, ProveedorGuiasEnvio> proveedores;
    private final TransactionTemplate transaccion;

    public ServicioGuiasEnvio(EnvioPedidoRepositorio envios, CheckoutOrdenRepositorio ordenes,
                              CheckoutOrdenDetalleRepositorio detalles,
                              EmpresaEnvioConfigRepositorio configuraciones,
                              SucursalEnvioOrigenRepositorio origenes, ProductoRepositorio productos,
                              ResolutorCredencialEnvio credenciales, ServicioTrazabilidadEnvio trazabilidad,
                              ServicioOutboxCorreoEnvios notificaciones,
                              List<ProveedorGuiasEnvio> proveedores, PlatformTransactionManager tx, java.time.Clock reloj) {
        this.reloj = reloj;
        this.envios = envios; this.ordenes = ordenes; this.detalles = detalles;
        this.configuraciones = configuraciones; this.origenes = origenes; this.productos = productos;
        this.credenciales = credenciales; this.trazabilidad = trazabilidad;
        this.notificaciones = notificaciones;
        this.proveedores = proveedores.stream().collect(Collectors.toUnmodifiableMap(
                item -> item.codigo().toUpperCase(Locale.ROOT), Function.identity()));
        this.transaccion = new TransactionTemplate(tx);
    }

    public boolean generarAutomaticamente(EnvioPedidoEntidad envio) {
        EmpresaEnvioConfigEntidad config = configuraciones.findById(envio.getEmpresaId()).orElse(null);
        if (config == null || !config.isHabilitado() || !config.isGenerarGuiaAutomaticamente()) return false;
        generar(envio.getEmpresaId(), envio.getId());
        return true;
    }

    public void generar(Long empresaId, Long envioId) {
        boolean reservado = Boolean.TRUE.equals(transaccion.execute(status -> {
            if (envios.reservarGeneracion(empresaId, envioId) == 0) return false;
            EnvioPedidoEntidad reservadoEnvio = envios.findById(envioId).orElseThrow();
            trazabilidad.registrar(reservadoEnvio, "GENERACION_INICIADA",
                    "Se inició la generación de la guía con el proveedor configurado", null);
            return true;
        }));
        if (!reservado) {
            EnvioPedidoEntidad actual = envio(empresaId, envioId);
            if (actual.getNumeroGuia() != null) return;
            throw error(HttpStatus.CONFLICT, "El envío no está listo para generar una guía");
        }

        try {
            Contexto contexto = contexto(empresaId, envioId);
            ProveedorGuiasEnvio proveedor = Optional.ofNullable(proveedores.get(
                            contexto.envio().getProveedor().toUpperCase(Locale.ROOT)))
                    .orElseThrow(() -> new IllegalStateException("No existe un adaptador para el proveedor configurado"));
            ProveedorGuiasEnvio.Guia guia = proveedor.generar(credenciales.resolver(contexto.config()),
                    contexto.envio().getAmbiente(), solicitud(contexto));
            transaccion.executeWithoutResult(status -> completar(contexto.envio().getId(), guia));
        } catch (RuntimeException ex) {
            transaccion.executeWithoutResult(status -> fallar(empresaId, envioId, ex));
            throw error(HttpStatus.BAD_GATEWAY, mensaje(ex));
        }
    }

    public void actualizarRastreo(EnvioPedidoEntidad envio) {
        try {
            EmpresaEnvioConfigEntidad config = configuraciones.findById(envio.getEmpresaId())
                    .filter(EmpresaEnvioConfigEntidad::isHabilitado).orElse(null);
            ProveedorGuiasEnvio proveedor = proveedores.get(envio.getProveedor().toUpperCase(Locale.ROOT));
            if (config == null || proveedor == null || envio.getNumeroGuia() == null) return;
            var rastreo = proveedor.rastrear(credenciales.resolver(config), envio.getAmbiente(), envio.getNumeroGuia());
            String estadoAnterior = envio.getEstado();
            String estado = normalizarEstado(rastreo.estado(), estadoAnterior);
            transaccion.executeWithoutResult(status -> {
                EnvioPedidoEntidad actual = envio(envio.getEmpresaId(), envio.getId());
                actual.setEstado(estado);
                actual.setEstadoRastreo(rastreo.estado());
                actual.setRastreoActualizadoEn(LocalDateTime.now(reloj));
                actual.setUltimoError(null);
                envios.save(actual);
                trazabilidad.registrar(actual, "RASTREO", rastreo.descripcion(), rastreo.referenciaExterna());
                if (!Objects.equals(estadoAnterior, estado)) {
                    notificaciones.programar(actual.getEmpresaId(), actual.getId(), estado);
                }
            });
        } catch (RuntimeException ex) {
            transaccion.executeWithoutResult(status -> {
                EnvioPedidoEntidad actual = envio(envio.getEmpresaId(), envio.getId());
                actual.setUltimoError(mensaje(ex));
                actual.setRastreoActualizadoEn(LocalDateTime.now(reloj));
                envios.save(actual);
            });
        }
    }

    public void aplicarEventoWebhook(Long empresaId, String proveedor, String proveedorEnvioId,
                                     String numeroGuia, String estadoProveedor, String descripcion,
                                     String referenciaExterna) {
        EnvioPedidoEntidad envio = null;
        if (proveedorEnvioId != null && !proveedorEnvioId.isBlank()) {
            envio = envios.findByEmpresaIdAndProveedorAndProveedorEnvioId(empresaId, proveedor, proveedorEnvioId)
                    .orElse(null);
        }
        if (envio == null && numeroGuia != null && !numeroGuia.isBlank()) {
            envio = envios.findByEmpresaIdAndProveedorAndNumeroGuia(empresaId, proveedor, numeroGuia)
                    .orElse(null);
        }
        if (envio == null) throw new IllegalArgumentException("El evento no corresponde a una guía del negocio");

        String estadoAnterior = envio.getEstado();
        String estado = normalizarEstado(estadoProveedor, estadoAnterior);
        envio.setEstado(estado);
        envio.setEstadoRastreo(estadoProveedor);
        envio.setRastreoActualizadoEn(LocalDateTime.now(reloj));
        envio.setUltimoError(null);
        envios.save(envio);
        String detalle = descripcion == null || descripcion.isBlank()
                ? "Envia.com actualizó el estado de la guía" : descripcion;
        trazabilidad.registrar(envio, "RASTREO_WEBHOOK", detalle, "webhook:" + referenciaExterna);
        if (!Objects.equals(estadoAnterior, estado)) {
            notificaciones.programar(envio.getEmpresaId(), envio.getId(), estado);
        }
    }

    private Contexto contexto(Long empresaId, Long envioId) {
        EnvioPedidoEntidad envio = envio(empresaId, envioId);
        CheckoutOrdenEntidad orden = ordenes.findById(envio.getCheckoutOrdenId())
                .orElseThrow(() -> new IllegalStateException("La orden del envío ya no existe"));
        EmpresaEnvioConfigEntidad config = configuraciones.findById(empresaId)
                .filter(EmpresaEnvioConfigEntidad::isHabilitado)
                .orElseThrow(() -> new IllegalStateException("La integración de envíos está inactiva"));
        SucursalEnvioOrigenEntidad origen = origenes.findByEmpresaIdOrderBySucursalIdAsc(empresaId).stream()
                .filter(item -> item.isActivo() && envio.getSucursalId().equals(item.getSucursalId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("La sucursal no tiene un origen activo"));
        List<CheckoutOrdenDetalleEntidad> lineas = detalles
                .findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(empresaId, orden.getId());
        Set<Long> ids = lineas.stream().map(CheckoutOrdenDetalleEntidad::getProductoId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ProductoEntidad> catalogo = productos.findByEmpresaIdAndIdIn(empresaId, ids).stream()
                .collect(Collectors.toMap(ProductoEntidad::getId, Function.identity()));
        return new Contexto(envio, orden, config, origen, lineas, catalogo);
    }

    private ProveedorGuiasEnvio.Solicitud solicitud(Contexto contexto) {
        var origen = contexto.origen(); var orden = contexto.orden();
        List<ProveedorGuiasEnvio.Paquete> paquetes = contexto.lineas().stream()
                .filter(linea -> linea.getProductoId() != null).map(linea -> {
                    ProductoEntidad producto = contexto.productos().get(linea.getProductoId());
                    if (producto == null) throw new IllegalStateException("Un producto de la orden ya no está disponible");
                    return new ProveedorGuiasEnvio.Paquete(producto.getNombre(), linea.getCantidad(), linea.getTotal(),
                            producto.getPesoKg(), producto.getLargoCm(), producto.getAnchoCm(), producto.getAltoCm());
                }).toList();
        if (paquetes.isEmpty()) throw new IllegalStateException("La orden no contiene productos para enviar");
        var direccionOrigen = new ProveedorGuiasEnvio.Direccion(origen.getNombreContacto(), null, origen.getCorreo(),
                origen.getTelefono(), origen.getCalle(), origen.getNumeroExterior(), origen.getColonia(),
                origen.getMunicipio(), origen.getEstado(), origen.getCodigoPais(), origen.getCodigoPostal(),
                origen.getReferencias());
        var direccionDestino = new ProveedorGuiasEnvio.Direccion(orden.getNombreDestinatario(), null,
                orden.getCorreoCliente(), orden.getTelefonoDestinatario(), orden.getCalle(), orden.getNumeroExterior(),
                orden.getColonia(), orden.getMunicipio(), orden.getEstadoDireccion(), orden.getCodigoPais(),
                orden.getCodigoPostal(), orden.getReferenciasDireccion());
        return new ProveedorGuiasEnvio.Solicitud(direccionOrigen, direccionDestino, paquetes, orden.getMoneda(),
                contexto.envio().getPaqueteria(), contexto.envio().getServicio(), orden.getPublicId());
    }

    private void completar(Long envioId, ProveedorGuiasEnvio.Guia guia) {
        EnvioPedidoEntidad envio = envios.findById(envioId).orElseThrow();
        envio.setProveedorEnvioId(guia.proveedorEnvioId());
        envio.setNumeroGuia(guia.numeroGuia());
        envio.setUrlEtiqueta(guia.urlEtiqueta());
        envio.setUrlRastreo(guia.urlRastreo());
        if (guia.costo() != null) envio.setCostoProveedor(guia.costo());
        envio.setEstado("GUIA_GENERADA");
        envio.setEstadoRastreo(guia.estadoProveedor());
        envio.setMotivoBloqueo(null);
        envio.setUltimoError(null);
        envio.setGuiaGeneradaEn(LocalDateTime.now(reloj));
        envio.setRastreoActualizadoEn(LocalDateTime.now(reloj));
        envios.save(envio);
        trazabilidad.registrar(envio, "GUIA_GENERADA", "La guía fue generada correctamente",
                "guia:" + guia.numeroGuia());
        notificaciones.programar(envio.getEmpresaId(), envio.getId(), envio.getEstado());
    }

    private void fallar(Long empresaId, Long envioId, RuntimeException ex) {
        EnvioPedidoEntidad envio = envio(empresaId, envioId);
        envio.setEstado("ERROR_GUIA");
        envio.setIntentos(envio.getIntentos() + 1);
        envio.setUltimoError(mensaje(ex));
        envio.setMotivoBloqueo("No fue posible generar la guía. Revisa los datos y vuelve a intentarlo");
        envios.save(envio);
        trazabilidad.registrar(envio, "ERROR_GENERACION", mensaje(ex), null);
    }

    private EnvioPedidoEntidad envio(Long empresaId, Long envioId) {
        return envios.findById(envioId).filter(item -> empresaId.equals(item.getEmpresaId()))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Envío no encontrado"));
    }

    private String normalizarEstado(String estadoProveedor, String actual) {
        if (estadoProveedor == null) return actual;
        String valor = estadoProveedor.toUpperCase(Locale.ROOT);
        if (valor.contains("DELIVER") || valor.contains("ENTREG")) return "ENTREGADO";
        if (valor.contains("CANCEL")) return "CANCELADO";
        if (valor.contains("EXCEPTION") || valor.contains("INCIDEN") || valor.contains("FAILED")) return "INCIDENCIA";
        if (valor.contains("TRANSIT") || valor.contains("PICK") || valor.contains("RECOG")) return "EN_TRANSITO";
        return actual;
    }

    private String mensaje(Throwable ex) {
        String mensaje = ex.getMessage();
        return mensaje == null || mensaje.isBlank() ? "Error no identificado del proveedor de envíos" : mensaje;
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    private record Contexto(EnvioPedidoEntidad envio, CheckoutOrdenEntidad orden,
                            EmpresaEnvioConfigEntidad config, SucursalEnvioOrigenEntidad origen,
                            List<CheckoutOrdenDetalleEntidad> lineas, Map<Long, ProductoEntidad> productos) {}
}
