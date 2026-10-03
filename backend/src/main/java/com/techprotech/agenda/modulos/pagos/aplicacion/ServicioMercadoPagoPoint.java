package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.caja.api.dto.VentaResponse;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCaja;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaRepositorio;
import com.techprotech.agenda.modulos.pagos.api.dto.*;
import com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.TerminalMercadoPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteMercadoPagoPoint;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.PropiedadesMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.OrdenPointCajaRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.TerminalMercadoPagoRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioMercadoPagoPoint {
    private final ClienteMercadoPagoPoint cliente;
    private final ServicioOAuthMercadoPago oauth;
    private final PropiedadesMercadoPago propiedades;
    private final TerminalMercadoPagoRepositorio terminales;
    private final OrdenPointCajaRepositorio ordenes;
    private final SucursalRepositorio sucursales;
    private final VentaRepositorio ventasRepositorio;
    private final ServicioVentasCaja ventas;
    private final ServicioConciliacionPagos conciliacion;
    private final ServicioComisionPlataforma comisiones;

    public ServicioMercadoPagoPoint(ClienteMercadoPagoPoint cliente, ServicioOAuthMercadoPago oauth,
                                    PropiedadesMercadoPago propiedades,
                                    TerminalMercadoPagoRepositorio terminales, OrdenPointCajaRepositorio ordenes,
                                    SucursalRepositorio sucursales, VentaRepositorio ventasRepositorio,
                                    ServicioVentasCaja ventas, ServicioConciliacionPagos conciliacion,
                                    ServicioComisionPlataforma comisiones) {
        this.cliente = cliente; this.oauth = oauth; this.propiedades = propiedades; this.terminales = terminales; this.ordenes = ordenes;
        this.sucursales = sucursales; this.ventasRepositorio = ventasRepositorio; this.ventas = ventas;
        this.conciliacion = conciliacion;
        this.comisiones = comisiones;
    }

    @Transactional(readOnly = true)
    public List<TerminalPointResponse> listarTerminales(Long empresaId) {
        Map<String, TerminalMercadoPagoEntidad> guardadas = terminales.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(TerminalMercadoPagoEntidad::getTerminalId, Function.identity()));
        Map<Long, String> nombresSucursal = sucursales.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        List<ClienteMercadoPagoPoint.TerminalRemota> remotas = new ArrayList<>(cliente.listarTerminales(empresaId));
        String terminalPruebaId = terminalPruebaId();
        if (oauth.esAmbientePrueba() && terminalPruebaId != null
                && remotas.stream().noneMatch(item -> terminalPruebaId.equals(item.terminalId()))) {
            remotas.add(new ClienteMercadoPagoPoint.TerminalRemota(terminalPruebaId, null, null, null, "PDV"));
        }
        Set<String> conocidas = remotas.stream().map(ClienteMercadoPagoPoint.TerminalRemota::terminalId).collect(Collectors.toSet());
        List<TerminalPointResponse> resultado = remotas.stream().map(remota -> mapear(remota, guardadas.get(remota.terminalId()), nombresSucursal)).collect(Collectors.toCollection(ArrayList::new));
        guardadas.values().stream().filter(item -> !conocidas.contains(item.getTerminalId()))
                .map(item -> mapear(null, item, nombresSucursal)).forEach(resultado::add);
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<TerminalPointResponse> listarTerminalesCaja(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
        if (sucursalId == null || sucursalesPermitidas != null && !sucursalesPermitidas.isEmpty() && !sucursalesPermitidas.contains(sucursalId)) {
            throw error(HttpStatus.FORBIDDEN, "No tienes acceso a la sucursal indicada");
        }
        Map<Long, String> nombres = sucursales.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        return terminales.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(TerminalMercadoPagoEntidad::isActiva).filter(item -> sucursalId.equals(item.getSucursalId()))
                .map(item -> mapear(null, item, nombres)).toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenPointResponse> listarOrdenesCaja(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
        if (sucursalId == null || sucursalesPermitidas != null && !sucursalesPermitidas.isEmpty() && !sucursalesPermitidas.contains(sucursalId)) {
            throw error(HttpStatus.FORBIDDEN, "No tienes acceso a la sucursal indicada");
        }
        Set<Long> idsTerminal = terminales.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(item -> sucursalId.equals(item.getSucursalId())).map(TerminalMercadoPagoEntidad::getId).collect(Collectors.toSet());
        return ordenes.findTop50ByEmpresaIdOrderByCreadoEnDesc(empresaId).stream()
                .filter(item -> idsTerminal.contains(item.getTerminalId())).map(this::mapear).toList();
    }

    @Transactional
    public TerminalPointResponse guardarTerminal(Long empresaId, GuardarTerminalPointRequest request) {
        SucursalEntidad sucursal = sucursales.findByIdAndEmpresaIdAndActivaTrue(request.sucursalId(), empresaId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "La sucursal no existe para este negocio"));
        ClienteMercadoPagoPoint.TerminalRemota remota = cliente.listarTerminales(empresaId).stream()
                .filter(item -> request.terminalId().equals(item.terminalId())).findFirst().orElse(null);
        if (remota == null && !(oauth.esAmbientePrueba() && request.terminalId().equals(terminalPruebaId()))) {
            throw error(HttpStatus.BAD_REQUEST, "La terminal no pertenece a la cuenta Mercado Pago conectada");
        }
        TerminalMercadoPagoEntidad entidad = terminales.findByEmpresaIdAndTerminalId(empresaId, request.terminalId())
                .orElseGet(TerminalMercadoPagoEntidad::new);
        entidad.setEmpresaId(empresaId); entidad.setSucursalId(sucursal.getId()); entidad.setTerminalId(request.terminalId());
        entidad.setNombre(request.nombre().trim()); entidad.setActiva(request.activa());
        entidad.setPosId(remota == null ? null : remota.posId()); entidad.setStoreId(remota == null ? null : remota.storeId());
        entidad.setExternalPosId(remota == null ? null : remota.externalPosId());
        entidad.setModoOperacion(remota == null ? "PDV" : remota.modoOperacion());
        entidad = terminales.save(entidad);
        return mapear(remota, entidad, Map.of(sucursal.getId(), sucursal.getNombre()));
    }

    @Transactional
    public OrdenPointResponse crearOrden(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas,
                                         String idempotencia, CrearOrdenPointRequest request) {
        validarIdempotencia(idempotencia);
        Optional<OrdenPointCajaEntidad> existente = ordenes.findByEmpresaIdAndIdempotencia(empresaId, idempotencia.trim());
        if (existente.isPresent()) return mapear(existente.get());
        TerminalMercadoPagoEntidad terminal = terminales.findByIdAndEmpresaId(request.terminalId(), empresaId)
                .filter(TerminalMercadoPagoEntidad::isActiva)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "La terminal Point no está disponible"));
        if (!terminal.getSucursalId().equals(request.venta().sucursalId())) {
            throw error(HttpStatus.BAD_REQUEST, "La terminal Point pertenece a otra sucursal");
        }
        VentaResponse venta = ventas.prepararPoint(empresaId, usuarioId, sucursalesPermitidas, request.venta());
        String publicId = UUID.randomUUID().toString();
        ClienteMercadoPagoPoint.OrdenRemota remota;
        try {
            remota = cliente.crearOrden(empresaId, idempotencia.trim(), terminal.getTerminalId(),
                    referencia(publicId), venta.total(),
                    comisiones.calcular(empresaId, ProveedorPago.MERCADO_PAGO, venta.total(), venta.moneda()),
                    "Venta " + venta.folio());
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("HTTP 409")) {
                throw error(HttpStatus.CONFLICT, "La terminal ya tiene un cobro pendiente. Retómalo o cancélalo desde Cobros Point antes de enviar otra cuenta");
            }
            throw ex;
        }
        OrdenPointCajaEntidad orden = new OrdenPointCajaEntidad();
        orden.setPublicId(publicId); orden.setEmpresaId(empresaId); orden.setVentaId(venta.id()); orden.setTerminalId(terminal.getId());
        orden.setProveedorOrdenId(remota.ordenId()); orden.setProveedorPagoId(remota.pagoId()); orden.setIdempotencia(idempotencia.trim());
        orden.setImporte(venta.total()); orden.setMoneda(venta.moneda()); orden.setCreadoPorUsuarioId(usuarioId);
        aplicarEstado(orden, remota);
        orden = ordenes.save(orden);
        conciliacion.registrarPoint(orden, remota);
        return mapear(orden);
    }

    @Transactional
    public OrdenPointResponse consultar(Long empresaId, String publicId) {
        OrdenPointCajaEntidad orden = buscar(empresaId, publicId);
        ClienteMercadoPagoPoint.OrdenRemota remota = cliente.consultar(empresaId, orden.getProveedorOrdenId());
        aplicarEstado(orden, remota);
        orden = ordenes.save(orden);
        conciliacion.registrarPoint(orden, remota);
        return mapear(orden);
    }

    @Transactional
    public OrdenPointResponse cancelar(Long empresaId, String publicId, String idempotencia) {
        validarIdempotencia(idempotencia);
        OrdenPointCajaEntidad orden = buscar(empresaId, publicId);
        if (Set.of("APROBADO", "REEMBOLSADO").contains(orden.getEstado())) throw error(HttpStatus.CONFLICT, "La orden ya fue cobrada");
        ClienteMercadoPagoPoint.OrdenRemota remota = cliente.cancelar(empresaId, orden.getProveedorOrdenId(), idempotencia);
        aplicarEstado(orden, remota);
        orden = ordenes.save(orden);
        conciliacion.registrarPoint(orden, remota);
        return mapear(orden);
    }

    @Transactional
    public OrdenPointResponse reembolsar(Long empresaId, String publicId, String idempotencia) {
        validarIdempotencia(idempotencia);
        OrdenPointCajaEntidad orden = buscar(empresaId, publicId);
        if (!"APROBADO".equals(orden.getEstado())) throw error(HttpStatus.CONFLICT, "Sólo se puede reembolsar un cobro Point aprobado");
        ClienteMercadoPagoPoint.OrdenRemota remota = cliente.reembolsar(empresaId, orden.getProveedorOrdenId(), idempotencia);
        aplicarEstado(orden, remota);
        orden = ordenes.save(orden);
        conciliacion.registrarPoint(orden, remota);
        return mapear(orden);
    }

    @Transactional
    public OrdenPointResponse simularAprobacion(Long empresaId, String publicId) {
        OrdenPointCajaEntidad orden = buscar(empresaId, publicId);
        TerminalMercadoPagoEntidad terminal = terminales.findByIdAndEmpresaId(orden.getTerminalId(), empresaId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "La terminal Point no está disponible"));
        if (!simulacionDisponible(terminal)) {
            throw error(HttpStatus.CONFLICT, "La simulación sólo está disponible con la terminal virtual en ambiente de prueba");
        }
        if (Set.of("APROBADO", "CANCELADO", "RECHAZADO", "EXPIRADO", "REEMBOLSADO").contains(orden.getEstado())) {
            throw error(HttpStatus.CONFLICT, "La orden Point ya terminó");
        }
        cliente.simularAprobacion(empresaId, orden.getProveedorOrdenId());
        ClienteMercadoPagoPoint.OrdenRemota remota = cliente.consultar(empresaId, orden.getProveedorOrdenId());
        aplicarEstado(orden, remota);
        orden = ordenes.save(orden);
        conciliacion.registrarPoint(orden, remota);
        return mapear(orden);
    }

    @Transactional
    public boolean procesarWebhook(String proveedorOrdenId) {
        OrdenPointCajaEntidad orden = ordenes.findByProveedorOrdenId(proveedorOrdenId).orElse(null);
        if (orden == null) return false;
        ClienteMercadoPagoPoint.OrdenRemota remota = cliente.consultar(orden.getEmpresaId(), proveedorOrdenId);
        aplicarEstado(orden, remota);
        orden = ordenes.save(orden);
        conciliacion.registrarPoint(orden, remota);
        return true;
    }

    private void aplicarEstado(OrdenPointCajaEntidad orden, ClienteMercadoPagoPoint.OrdenRemota remota) {
        orden.setProveedorOrdenId(remota.ordenId());
        if (remota.pagoId() != null) orden.setProveedorPagoId(remota.pagoId());
        String estado = normalizarEstado(remota.estado(), remota.estadoPago(), remota.detallePago());
        orden.setEstado(estado); orden.setDetalleEstado(remota.detallePago() != null ? remota.detallePago() : remota.detalleEstado());
        if (orden.getVentaId() == null) return;
        if ("APROBADO".equals(estado)) ventas.confirmarPoint(orden.getEmpresaId(), orden.getVentaId(), remota.pagoId());
        if (Set.of("CANCELADO", "RECHAZADO", "EXPIRADO").contains(estado)) ventas.cancelarPoint(orden.getEmpresaId(), orden.getVentaId(), orden.getDetalleEstado());
        if ("REEMBOLSADO".equals(estado)) ventas.marcarReembolsadaPoint(orden.getEmpresaId(), orden.getVentaId());
    }

    private String normalizarEstado(String orden, String pago, String detalle) {
        String combinado = String.join(" ", valor(orden), valor(pago), valor(detalle)).toLowerCase(Locale.ROOT);
        if (combinado.contains("refund")) return "REEMBOLSADO";
        if (combinado.contains("reject") || combinado.contains("fail")) return "RECHAZADO";
        if (combinado.contains("cancel")) return "CANCELADO";
        if (combinado.contains("expired")) return "EXPIRADO";
        if (combinado.contains("processed") || combinado.contains("approved") || combinado.contains("accredited")) return "APROBADO";
        if (combinado.contains("at_terminal")) return "EN_TERMINAL";
        if (combinado.contains("action_required")) return "REQUIERE_ACCION";
        return "CREADO";
    }

    private OrdenPointResponse mapear(OrdenPointCajaEntidad orden) {
        VentaEntidad venta = ventasRepositorio.findByIdAndEmpresaId(orden.getVentaId(), orden.getEmpresaId()).orElse(null);
        TerminalMercadoPagoEntidad terminal = terminales.findByIdAndEmpresaId(orden.getTerminalId(), orden.getEmpresaId()).orElse(null);
        return new OrdenPointResponse(orden.getPublicId(), orden.getVentaId(), venta == null ? null : venta.getFolio(), orden.getTerminalId(),
                terminal == null ? null : terminal.getNombre(), orden.getEstado(), orden.getDetalleEstado(), orden.getImporte(), orden.getMoneda(),
                orden.getProveedorOrdenId(), orden.getProveedorPagoId(), orden.getCreadoEn(), orden.getActualizadoEn(), simulacionDisponible(terminal));
    }

    private TerminalPointResponse mapear(ClienteMercadoPagoPoint.TerminalRemota remota, TerminalMercadoPagoEntidad local,
                                          Map<Long, String> sucursales) {
        String terminalId = remota != null ? remota.terminalId() : local.getTerminalId();
        String serie = terminalId == null ? "Terminal" : terminalId.substring(terminalId.lastIndexOf("__") + 2);
        return new TerminalPointResponse(local == null ? null : local.getId(), terminalId,
                remota != null ? remota.posId() : local.getPosId(), remota != null ? remota.storeId() : local.getStoreId(),
                remota != null ? remota.externalPosId() : local.getExternalPosId(), local == null ? "Point " + serie : local.getNombre(),
                remota != null ? remota.modoOperacion() : local.getModoOperacion(), local == null ? null : local.getSucursalId(),
                local == null ? null : sucursales.get(local.getSucursalId()), local != null && local.isActiva(), local != null);
    }

    private OrdenPointCajaEntidad buscar(Long empresaId, String publicId) { return ordenes.findByEmpresaIdAndPublicId(empresaId, publicId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Orden Point no encontrada")); }
    private String terminalPruebaId() {
        String id = propiedades.pointTerminalPruebaId();
        return id == null || id.isBlank() ? null : id.trim();
    }
    private boolean simulacionDisponible(TerminalMercadoPagoEntidad terminal) {
        return terminal != null && oauth.esAmbientePrueba() && Objects.equals(terminal.getTerminalId(), terminalPruebaId());
    }
    private String referencia(String publicId) { return "point_" + publicId.replace("-", ""); }
    private String valor(String value) { return value == null ? "" : value; }
    private void validarIdempotencia(String value) { if (value == null || value.isBlank() || value.length() > 128) throw error(HttpStatus.BAD_REQUEST, "Idempotency-Key es obligatorio"); }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
