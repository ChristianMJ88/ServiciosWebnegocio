package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutPublicaResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.CrearPagoCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.PagoCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.dominio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ServicioPagoCheckout {
    private static final BigDecimal SIN_COMISION = new BigDecimal("0.00");

    private final ServicioConfiguracionCheckout configuracionCheckout;
    private final EmpresaSitioConfigRepositorio sitios;
    private final EmpresaCheckoutConfigRepositorio configuraciones;
    private final ProveedorPagoRepositorio proveedores;
    private final CheckoutOrdenRepositorio ordenes;
    private final CheckoutOrdenDetalleRepositorio detalles;
    private final IntentoPagoCheckoutRepositorio intentos;
    private final List<ProveedorPagosPort> adaptadores;
    private final ServicioInventarioCheckout inventario;
    private final ServicioEstadoPagoCheckout estados;
    private final ServicioMetodosPagoCheckout metodosPago;

    public ServicioPagoCheckout(ServicioConfiguracionCheckout configuracionCheckout,
                                EmpresaSitioConfigRepositorio sitios,
                                EmpresaCheckoutConfigRepositorio configuraciones,
                                ProveedorPagoRepositorio proveedores,
                                CheckoutOrdenRepositorio ordenes,
                                CheckoutOrdenDetalleRepositorio detalles,
                                IntentoPagoCheckoutRepositorio intentos,
                                List<ProveedorPagosPort> adaptadores,
                                ServicioInventarioCheckout inventario,
                                ServicioEstadoPagoCheckout estados,
                                ServicioMetodosPagoCheckout metodosPago) {
        this.configuracionCheckout = configuracionCheckout;
        this.sitios = sitios;
        this.configuraciones = configuraciones;
        this.proveedores = proveedores;
        this.ordenes = ordenes;
        this.detalles = detalles;
        this.intentos = intentos;
        this.adaptadores = adaptadores;
        this.inventario = inventario;
        this.estados = estados;
        this.metodosPago = metodosPago;
    }

    public PagoCheckoutResponse pagarConTarjeta(String slug, String ordenPublicId, String idempotencia,
                                                 CrearPagoCheckoutRequest request) {
        validarIdempotencia(idempotencia);
        EmpresaSitioConfigEntidad sitio = sitio(slug);
        String clave = idempotencia.trim();
        var existente = intentos.findByEmpresaIdAndIdempotencia(sitio.getEmpresaId(), clave);
        if (existente.isPresent()) return respuestaIdempotente(ordenPublicId, existente.get());

        ConfiguracionCheckoutPublicaResponse config = configuracionCheckout.obtenerPublica(slug);
        if (!config.habilitado() || !config.aceptarTarjeta()) {
            throw error(HttpStatus.CONFLICT, "El pago con tarjeta no está habilitado para este negocio");
        }
        CheckoutOrdenEntidad orden = ordenes.findByEmpresaIdAndPublicId(sitio.getEmpresaId(), ordenPublicId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Orden no encontrada"));
        validarOrden(orden, request, config);

        EmpresaCheckoutConfigEntidad checkout = configuraciones.findById(sitio.getEmpresaId())
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "El checkout no tiene un proveedor configurado"));
        ProveedorPagoEntidad proveedor = proveedores.findById(checkout.getProveedorPredeterminadoId())
                .filter(ProveedorPagoEntidad::isActivo)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "El proveedor de pago no está disponible"));
        ProveedorPago proveedorDominio = proveedor(proveedor.getCodigo());
        ProveedorPagosPort adaptador = adaptadores.stream().filter(item -> item.proveedor() == proveedorDominio)
                .findFirst().orElseThrow(() -> error(HttpStatus.SERVICE_UNAVAILABLE, "El proveedor de pago no está disponible"));

        inventario.reservar(orden.getEmpresaId(), orden.getId());
        IntentoPagoCheckoutEntidad intento = nuevoIntento(orden, proveedor, clave,
                request.metodoPagoId().trim(), request.tipoPagoId());
        intentos.save(intento);

        ResultadoCobro resultado;
        try {
            Map<String, String> metadatos = new LinkedHashMap<>();
            metadatos.put("payment_method_id", request.metodoPagoId().trim());
            if (request.tipoPagoId() != null && !request.tipoPagoId().isBlank()) {
                metadatos.put("payment_type_id", request.tipoPagoId().trim());
            }
            metadatos.put("installments", String.valueOf(request.parcialidades()));
            metadatos.put("description", "Orden " + orden.getPublicId());
            resultado = adaptador.crearCobro(new SolicitudCobro(
                    sitio.getEmpresaId(), orden.getPublicId(), clave, orden.getTotal(), orden.getMoneda(),
                    SIN_COMISION, request.tokenMedioPago().trim(), orden.getCorreoCliente(), metadatos));
        } catch (IllegalArgumentException ex) {
            registrarError(intento, "SOLICITUD_INVALIDA", ex.getMessage());
            inventario.liberar(orden.getEmpresaId(), orden.getId());
            throw error(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            registrarError(intento, "ERROR_PROVEEDOR", ex.getMessage());
            inventario.liberar(orden.getEmpresaId(), orden.getId());
            throw error(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }

        ServicioEstadoPagoCheckout.EstadoAplicado aplicado = estados.aplicar(intento.getId(), resultado);
        return respuesta(aplicado.ordenPublicId(), aplicado.intento(), aplicado.estadoOrden(), proveedor.getCodigo());
    }

    public PagoCheckoutResponse pagarConMetodoAlternativo(String slug, String ordenPublicId, String idempotencia,
                                                           com.techprotech.agenda.modulos.pagos.api.dto.CrearPagoAlternativoCheckoutRequest request) {
        validarIdempotencia(idempotencia);
        EmpresaSitioConfigEntidad sitio = sitio(slug);
        String clave = idempotencia.trim();
        var existente = intentos.findByEmpresaIdAndIdempotencia(sitio.getEmpresaId(), clave);
        if (existente.isPresent()) return respuestaIdempotente(ordenPublicId, existente.get());

        ConfiguracionCheckoutPublicaResponse config = configuracionCheckout.obtenerPublica(slug);
        if (!config.habilitado() || !config.permitirPagoProductos()) {
            throw error(HttpStatus.CONFLICT, "El pago de productos no está habilitado para este negocio");
        }
        CheckoutOrdenEntidad orden = ordenes.findByEmpresaIdAndPublicId(sitio.getEmpresaId(), ordenPublicId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Orden no encontrada"));
        validarOrden(orden, request.correoPagador(), config);
        var conceptos = detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(orden.getEmpresaId(), orden.getId());
        if (conceptos.isEmpty() || conceptos.stream().anyMatch(item -> !"PRODUCTO".equals(item.getTipoConcepto()))) {
            throw error(HttpStatus.CONFLICT, "OXXO, transferencias y pagos referenciados están disponibles solo para productos");
        }

        EmpresaMetodoPagoCheckoutEntidad metodo = metodosPago.obtenerActivo(sitio.getEmpresaId(), request.metodoPagoId());
        boolean transferencia = "bank_transfer".equalsIgnoreCase(metodo.getTipoExterno());
        if ((transferencia && !config.aceptarTransferencia())
                || (!transferencia && !config.aceptarEfectivoReferenciado())) {
            throw error(HttpStatus.CONFLICT, "El medio de pago está deshabilitado para este negocio");
        }

        EmpresaCheckoutConfigEntidad checkout = configuraciones.findById(sitio.getEmpresaId())
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "El checkout no tiene un proveedor configurado"));
        ProveedorPagoEntidad proveedor = proveedores.findById(checkout.getProveedorPredeterminadoId())
                .filter(ProveedorPagoEntidad::isActivo)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "El proveedor de pago no está disponible"));
        ProveedorPagosPort adaptador = adaptadores.stream().filter(item -> item.proveedor() == proveedor(proveedor.getCodigo()))
                .findFirst().orElseThrow(() -> error(HttpStatus.SERVICE_UNAVAILABLE, "El proveedor de pago no está disponible"));

        inventario.reservar(orden.getEmpresaId(), orden.getId());
        orden.setExpiraEn(LocalDateTime.now(ZoneOffset.UTC).plusDays(metodo.getVigenciaDias()));
        ordenes.save(orden);
        IntentoPagoCheckoutEntidad intento = nuevoIntento(orden, proveedor, clave,
                metodo.getMetodoExternoId(), metodo.getTipoExterno());
        intentos.save(intento);

        ResultadoCobro resultado;
        try {
            Map<String, String> metadatos = new LinkedHashMap<>();
            metadatos.put("payment_method_id", metodo.getMetodoExternoId());
            metadatos.put("payment_type_id", metodo.getTipoExterno());
            metadatos.put("expiration_time", "P" + metodo.getVigenciaDias() + "D");
            metadatos.put("description", "Orden " + orden.getPublicId());
            resultado = adaptador.crearCobro(new SolicitudCobro(sitio.getEmpresaId(), orden.getPublicId(), clave,
                    orden.getTotal(), orden.getMoneda(), SIN_COMISION, null,
                    request.correoPagador().trim().toLowerCase(Locale.ROOT), metadatos));
        } catch (IllegalArgumentException ex) {
            registrarError(intento, "SOLICITUD_INVALIDA", ex.getMessage());
            inventario.liberar(orden.getEmpresaId(), orden.getId());
            throw error(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            registrarError(intento, "ERROR_PROVEEDOR", ex.getMessage());
            inventario.liberar(orden.getEmpresaId(), orden.getId());
            throw error(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
        ServicioEstadoPagoCheckout.EstadoAplicado aplicado = estados.aplicar(intento.getId(), resultado);
        return respuesta(aplicado.ordenPublicId(), aplicado.intento(), aplicado.estadoOrden(), proveedor.getCodigo());
    }

    private IntentoPagoCheckoutEntidad nuevoIntento(CheckoutOrdenEntidad orden, ProveedorPagoEntidad proveedor,
                                                     String idempotencia, String metodoPagoId, String tipoPagoId) {
        IntentoPagoCheckoutEntidad intento = new IntentoPagoCheckoutEntidad();
        intento.setCheckoutOrdenId(orden.getId());
        intento.setEmpresaId(orden.getEmpresaId());
        intento.setProveedorPagoId(proveedor.getId());
        intento.setMetodoPagoId(metodoPagoId);
        intento.setTipoPagoId(tipoPagoId);
        intento.setIdempotencia(idempotencia);
        intento.setEstado(EstadoPago.CREADO.name());
        intento.setImporte(orden.getTotal());
        intento.setMoneda(orden.getMoneda());
        return intento;
    }

    private void registrarError(IntentoPagoCheckoutEntidad intento, String codigo, String detalle) {
        intento.setEstado("ERROR");
        intento.setCodigoRespuesta(codigo);
        intento.setDetalleRespuesta(limitar(detalle, 500));
        intentos.save(intento);
    }

    private void validarOrden(CheckoutOrdenEntidad orden, CrearPagoCheckoutRequest request,
                              ConfiguracionCheckoutPublicaResponse config) {
        validarOrden(orden, request.correoPagador(), config);
    }

    private void validarOrden(CheckoutOrdenEntidad orden, String correoPagador,
                              ConfiguracionCheckoutPublicaResponse config) {
        if (!"PENDIENTE_PAGO".equals(orden.getEstado())) {
            throw error(HttpStatus.CONFLICT, "La orden ya no está disponible para iniciar un pago");
        }
        if (!orden.getExpiraEn().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw error(HttpStatus.CONFLICT, "La orden expiró; vuelve a confirmar el carrito");
        }
        if (!orden.getMoneda().equalsIgnoreCase(config.moneda())) {
            throw error(HttpStatus.CONFLICT, "La moneda de la orden ya no coincide con la configuración del checkout");
        }
        if (!orden.getCorreoCliente().equalsIgnoreCase(correoPagador.trim())) {
            throw error(HttpStatus.BAD_REQUEST, "El correo del pagador debe coincidir con el de la orden");
        }
    }

    private PagoCheckoutResponse respuestaIdempotente(String ordenPublicId, IntentoPagoCheckoutEntidad intento) {
        CheckoutOrdenEntidad orden = ordenes.findById(intento.getCheckoutOrdenId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Orden no encontrada"));
        if (!orden.getPublicId().equals(ordenPublicId)) {
            throw error(HttpStatus.CONFLICT, "La clave de idempotencia pertenece a otra orden");
        }
        ProveedorPagoEntidad proveedor = proveedores.findById(intento.getProveedorPagoId())
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "Proveedor no disponible"));
        return respuesta(ordenPublicId, intento, orden.getEstado(), proveedor.getCodigo());
    }

    private PagoCheckoutResponse respuesta(String ordenPublicId, IntentoPagoCheckoutEntidad intento,
                                            String estadoOrden, String proveedor) {
        return new PagoCheckoutResponse(ordenPublicId, estadoOrden, intento.getEstado(), proveedor,
                intento.getReferenciaExterna(), intento.getReferenciaPagoExterna(), intento.getImporte(), intento.getMoneda(), intento.getAccionUrl(),
                intento.getCodigoRespuesta(), intento.getDetalleRespuesta());
    }

    private EmpresaSitioConfigEntidad sitio(String slug) {
        return sitios.findBySlug(slug.trim().toLowerCase(Locale.ROOT)).filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe un sitio publicado para ese tenant"));
    }

    private ProveedorPago proveedor(String codigo) {
        try { return ProveedorPago.valueOf(codigo); }
        catch (IllegalArgumentException ex) { throw error(HttpStatus.CONFLICT, "Proveedor de pago no soportado"); }
    }

    private void validarIdempotencia(String valor) {
        if (valor == null || valor.isBlank() || valor.length() > 128) {
            throw error(HttpStatus.BAD_REQUEST, "Idempotency-Key es obligatorio y debe tener máximo 128 caracteres");
        }
    }

    private String limitar(String valor, int maximo) {
        return valor == null || valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
