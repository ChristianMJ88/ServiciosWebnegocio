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
@lombok.extern.slf4j.Slf4j
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
    private final PagosPrevistosPoint previstos;
    private final java.time.Clock reloj;
    private final java.time.Duration limiteReenvio;
    private final org.springframework.transaction.support.TransactionTemplate transaccion;

    public ServicioMercadoPagoPoint(ClienteMercadoPagoPoint cliente, ServicioOAuthMercadoPago oauth,
                                    PropiedadesMercadoPago propiedades,
                                    TerminalMercadoPagoRepositorio terminales, OrdenPointCajaRepositorio ordenes,
                                    SucursalRepositorio sucursales, VentaRepositorio ventasRepositorio,
                                    ServicioVentasCaja ventas, ServicioConciliacionPagos conciliacion,
                                    ServicioComisionPlataforma comisiones, org.springframework.transaction.PlatformTransactionManager gestor, java.time.Clock reloj,
                                    @org.springframework.beans.factory.annotation.Value("${aplicacion.pagos.mercado-pago.point.limite-reenvio}") java.time.Duration limiteReenvio, PagosPrevistosPoint previstos) {
        this.cliente = cliente; this.oauth = oauth; this.propiedades = propiedades; this.terminales = terminales; this.ordenes = ordenes;
        this.sucursales = sucursales; this.ventasRepositorio = ventasRepositorio; this.ventas = ventas;
        this.conciliacion = conciliacion;
        this.comisiones = comisiones; this.reloj=reloj; this.limiteReenvio=limiteReenvio;this.previstos=previstos;
        this.transaccion = new org.springframework.transaction.support.TransactionTemplate(gestor);
        this.transaccion.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
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
        if(idsTerminal.isEmpty()) return List.of();
        var lista=new java.util.LinkedHashMap<Long,OrdenPointCajaEntidad>();
        ordenes.noFinales(empresaId,idsTerminal).forEach(o -> lista.put(o.getId(),o));
        ordenes.findTop50ByEmpresaIdAndTerminalIdInOrderByCreadoEnDesc(empresaId,idsTerminal).forEach(o -> lista.putIfAbsent(o.getId(),o));
        return lista.values().stream().map(this::mapear).toList();
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

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse crearOrden(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas,
                                         String idempotencia, CrearOrdenPointRequest request) {
        validarIdempotencia(idempotencia);
        var orden = transaccion.execute(status -> {
            // Serializa la idempotencia y la reserva por terminal antes de tomar citas.
            var terminal = terminales.bloquear(request.terminalId(),empresaId)
                    .filter(TerminalMercadoPagoEntidad::isActiva)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND,"La terminal Point no está disponible"));
            if (!terminal.getSucursalId().equals(request.venta().sucursalId()) ||
                    sucursalesPermitidas != null && !sucursalesPermitidas.isEmpty() && !sucursalesPermitidas.contains(terminal.getSucursalId()))
                throw error(HttpStatus.FORBIDDEN,"La terminal Point pertenece a otra sucursal");
            var existente = ordenes.findByEmpresaIdAndIdempotencia(empresaId,idempotencia.trim());
            if (existente.isPresent()) {
                if (!terminal.getId().equals(existente.get().getTerminalId())) throw error(HttpStatus.CONFLICT,"La clave ya corresponde a otra terminal");
                if(!previstos.mismaSolicitud(existente.get().getPagosPrevistosJson(),request.venta())) throw error(HttpStatus.CONFLICT,"La clave ya se utilizó con otra cuenta. Retoma la orden existente sin cambiar su solicitud");
                return new Preparada(existente.get(),false);
            }
            if (ordenes.existeEnCurso(empresaId,terminal.getId()))
                throw error(HttpStatus.CONFLICT,"La terminal ya tiene un cobro pendiente. Retómalo o cancélalo desde Cobros Point antes de enviar otra cuenta");
            var venta = ventas.prepararPoint(empresaId,usuarioId,sucursalesPermitidas,request.venta());
            var nueva = new OrdenPointCajaEntidad();
            nueva.setPublicId(UUID.randomUUID().toString()); nueva.setEmpresaId(empresaId); nueva.setVentaId(venta.id());
            nueva.setTerminalId(terminal.getId()); nueva.setIdempotencia(idempotencia.trim()); nueva.setEstado("CREANDO");
            var plan=com.techprotech.agenda.modulos.caja.aplicacion.PlanPagosVenta.resolver(venta.total(),request.venta(),true);
            nueva.setPagosPrevistosJson(previstos.escribir(plan,request.venta()));
            nueva.setImporte(plan.importePoint()); nueva.setMoneda(venta.moneda()); nueva.setCreadoPorUsuarioId(usuarioId);
            return new Preparada(ordenes.save(nueva),true);
        });
        if (!"CREANDO".equals(orden.orden().getEstado())) return mapear(orden.orden());
        return enviarCreacion(orden.orden(),orden.primerEnvio());
    }

    private record Preparada(OrdenPointCajaEntidad orden, boolean primerEnvio) {}

    private OrdenPointResponse enviarCreacion(OrdenPointCajaEntidad orden, boolean primerEnvio) {
        if (!primerEnvio && orden.getCreadoEn()!=null && orden.getCreadoEn().plus(limiteReenvio).isBefore(java.time.LocalDateTime.now(reloj))) {
            return revisarIndeterminada(orden);
        }
        ClienteMercadoPagoPoint.OrdenRemota remota;
        try {
            var terminal = terminales.findByIdAndEmpresaId(orden.getTerminalId(),orden.getEmpresaId()).orElseThrow();
            var venta = ventasRepositorio.findByIdAndEmpresaId(orden.getVentaId(),orden.getEmpresaId()).orElseThrow();
            // Sin transacción JDBC ni locks durante la red. Repetir usa la misma clave y referencia.
            remota = cliente.crearOrden(orden.getEmpresaId(),orden.getIdempotencia(),terminal.getTerminalId(),
                    referencia(orden.getPublicId()),orden.getImporte(),
                    comisiones.calcular(orden.getEmpresaId(),ProveedorPago.MERCADO_PAGO,orden.getImporte(),orden.getMoneda()),"Venta " + venta.getFolio());
        } catch (RuntimeException ex) {
            // Timeout, 5xx, respuesta incompleta o cualquier fallo de reenvío no prueban ausencia de cobro.
            if (!primerEnvio || !(ex instanceof com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ErrorProveedorPoint proveedor) || !proveedor.definitivo()) {
                log.warn("Resultado Point indeterminado: empresa={}, orden={}, tipo={}",orden.getEmpresaId(),orden.getPublicId(),ex.getClass().getSimpleName());
                return mapear(orden);
            }
            transaccion.executeWithoutResult(status -> {
                var actual = ordenes.bloquear(orden.getEmpresaId(),orden.getPublicId()).orElseThrow();
                if ("CREANDO".equals(actual.getEstado())) {
                    actual.setEstado("CANCELADO"); actual.setDetalleEstado("No se pudo crear el cobro Point");
                    ventas.cancelarPoint(actual.getEmpresaId(),actual.getVentaId(),actual.getDetalleEstado());
                    ordenes.save(actual);
                }
            });
            if (ex instanceof com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ErrorProveedorPoint p && p.status()==409)
                throw error(HttpStatus.CONFLICT,"La terminal ya tiene un cobro pendiente. Retómalo o cancélalo desde Cobros Point antes de enviar otra cuenta");
            throw ex;
        }
        return guardarRespuesta(orden,remota);
    }

    private OrdenPointResponse revisarIndeterminada(OrdenPointCajaEntidad orden) {
        return transaccion.execute(status -> {
            var actual=ordenes.bloquear(orden.getEmpresaId(),orden.getPublicId()).orElseThrow();
            if("CREANDO".equals(actual.getEstado())) {actual.setEstado("REQUIERE_REVISION");actual.setDetalleEstado("Verifica en Mercado Pago si se cobró antes de liberar");ordenes.save(actual);}
            return mapear(actual);
        });
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse reintentar(Long empresaId,List<Long> permitidas,String publicId) {
        var orden=buscarConAlcance(empresaId,permitidas,publicId);
        if(!"CREANDO".equals(orden.getEstado()) || orden.getProveedorOrdenId()!=null) throw error(HttpStatus.CONFLICT,"La orden no está pendiente de envío");
        return enviarCreacion(orden,false);
    }

    OrdenPointCajaEntidad buscarConAlcance(Long empresaId,List<Long> permitidas,String publicId) {
        var orden=buscar(empresaId,publicId);
        var terminal=terminales.findByIdAndEmpresaId(orden.getTerminalId(),empresaId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Terminal no disponible"));
        if(permitidas!=null && !permitidas.isEmpty() && !permitidas.contains(terminal.getSucursalId())) throw error(HttpStatus.FORBIDDEN,"No tienes acceso a la sucursal de esta orden Point");
        return orden;
    }

    private OrdenPointResponse guardarRespuesta(OrdenPointCajaEntidad orden, ClienteMercadoPagoPoint.OrdenRemota remota) {
        return transaccion.execute(status -> {
            var actual = ordenes.bloquear(orden.getEmpresaId(),orden.getPublicId()).orElseThrow();
            aplicarEstado(actual,remota);
            ordenes.save(actual); conciliacion.registrarPoint(actual,remota);
            return mapear(actual);
        });
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse consultar(Long empresaId, String publicId) {return consultar(empresaId,List.of(),publicId);}

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse consultar(Long empresaId,List<Long> permitidas, String publicId) {
        var orden=buscarConAlcance(empresaId,permitidas,publicId);
        if ("CREANDO".equals(orden.getEstado()) && orden.getProveedorOrdenId()==null) {
            if(orden.getCreadoEn()!=null && orden.getCreadoEn().plus(limiteReenvio).isBefore(java.time.LocalDateTime.now(reloj)))
                return revisarIndeterminada(orden);
            return mapear(orden);
        }
        if (orden.getProveedorOrdenId()==null) return mapear(orden);
        return guardarRespuesta(orden,cliente.consultar(empresaId,orden.getProveedorOrdenId()));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse cancelar(Long empresaId, String publicId, String idempotencia) {return cancelar(empresaId,List.of(),publicId,idempotencia);}

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse cancelar(Long empresaId,List<Long> permitidas, String publicId, String idempotencia) {
        validarIdempotencia(idempotencia);
        var orden=buscarConAlcance(empresaId,permitidas,publicId);
        if (Set.of("APROBADO","REEMBOLSADO").contains(orden.getEstado())) throw error(HttpStatus.CONFLICT,"La orden ya fue cobrada");
        if (orden.getProveedorOrdenId()==null) throw error(HttpStatus.CONFLICT,"El resultado del envío a Point está pendiente. Consulta la orden antes de cancelarla");
        return guardarRespuesta(orden,cliente.cancelar(empresaId,orden.getProveedorOrdenId(),idempotencia));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse reembolsar(Long empresaId,String publicId,String idempotencia) {return reembolsar(empresaId,List.of(),publicId,idempotencia);}

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse reembolsar(Long empresaId,List<Long> permitidas,String publicId,String idempotencia) {
        validarIdempotencia(idempotencia);
        var orden=buscarConAlcance(empresaId,permitidas,publicId);
        if (!"APROBADO".equals(orden.getEstado())) throw error(HttpStatus.CONFLICT,"Sólo se puede reembolsar un cobro Point aprobado");
        if (!previstos.leer(orden.getPagosPrevistosJson()).isEmpty()) throw error(HttpStatus.CONFLICT,"El pago dividido requiere revisión del reembolso de todos los métodos; no se reembolsa solo la parte Point desde esta acción");
        return guardarRespuesta(orden,cliente.reembolsar(empresaId,orden.getProveedorOrdenId(),idempotencia));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse simularAprobacion(Long empresaId,String publicId) {return simularAprobacion(empresaId,List.of(),publicId);}

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public OrdenPointResponse simularAprobacion(Long empresaId,List<Long> permitidas,String publicId) {
        var orden=buscarConAlcance(empresaId,permitidas,publicId);
        var terminal=terminales.findByIdAndEmpresaId(orden.getTerminalId(),empresaId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"La terminal Point no está disponible"));
        if (!simulacionDisponible(terminal)) throw error(HttpStatus.CONFLICT,"La simulación sólo está disponible con la terminal virtual en ambiente de prueba");
        if (Set.of("APROBADO","CANCELADO","RECHAZADO","EXPIRADO","REEMBOLSADO").contains(orden.getEstado())) throw error(HttpStatus.CONFLICT,"La orden Point ya terminó");
        if (orden.getProveedorOrdenId()==null) throw error(HttpStatus.CONFLICT,"Consulta primero el resultado del envío a Point");
        cliente.simularAprobacion(empresaId,orden.getProveedorOrdenId());
        return guardarRespuesta(orden,cliente.consultar(empresaId,orden.getProveedorOrdenId()));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public boolean procesarWebhook(String proveedorOrdenId) {
        var orden=ordenes.findByProveedorOrdenId(proveedorOrdenId).orElse(null);
        if (orden==null) return false;
        guardarRespuesta(orden,cliente.consultar(orden.getEmpresaId(),proveedorOrdenId));
        return true;
    }

    void aplicarEstado(OrdenPointCajaEntidad orden, ClienteMercadoPagoPoint.OrdenRemota remota) {
        orden.setProveedorOrdenId(remota.ordenId());
        if (remota.pagoId() != null) orden.setProveedorPagoId(remota.pagoId());
        String estado = normalizarEstado(remota.estado(), remota.estadoPago(), remota.detallePago());
        // Una notificación atrasada no revierte un cobro acreditado.
        if ("REEMBOLSADO".equals(orden.getEstado()) || "APROBADO".equals(orden.getEstado()) && !"REEMBOLSADO".equals(estado)) return;
        if(Set.of("CANCELADO","RECHAZADO","EXPIRADO").contains(orden.getEstado()) && !Set.of("APROBADO","REEMBOLSADO").contains(estado)) return;
        orden.setEstado(estado); orden.setDetalleEstado(remota.detallePago() != null ? remota.detallePago() : remota.detalleEstado());
        if (orden.getVentaId() == null) return;
        if ("APROBADO".equals(estado)) {
            java.util.List<com.techprotech.agenda.modulos.caja.aplicacion.PlanPagosVenta.Parte> partes;boolean invalido=false;
            try{partes=previstos.leer(orden.getPagosPrevistosJson());}catch(IllegalStateException e){partes=List.of();invalido=true;}
            var resultado=ventas.confirmarPoint(orden,remota.pagoId()==null?remota.ordenId():remota.pagoId(),partes);
            if(resultado.requiereRevision() || invalido) orden.setDetalleEstado("REVISAR: Pago aprobado con incidencia. Verifica la conciliación antes de registrar otro cobro");
        }
        if (Set.of("CANCELADO", "RECHAZADO", "EXPIRADO").contains(estado)) ventas.cancelarPoint(orden.getEmpresaId(), orden.getVentaId(), orden.getDetalleEstado());
        if ("REEMBOLSADO".equals(estado)) {
            boolean simple;try{simple=previstos.leer(orden.getPagosPrevistosJson()).isEmpty();}catch(IllegalStateException e){simple=false;}
            if(simple) {if(!ventas.marcarReembolsadaPoint(orden)) orden.setDetalleEstado("REVISAR: Reembolso de orden no acreditada. Se conservan la venta y los otros cobros");}
            else {ventas.revisarReembolsoPoint(orden.getEmpresaId(),orden.getVentaId());orden.setDetalleEstado("REVISAR: Reembolso parcial externo. El efectivo y la venta se conservan hasta la revisión completa");}
        }
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

    OrdenPointResponse mapear(OrdenPointCajaEntidad orden) {
        VentaEntidad venta = ventasRepositorio.findByIdAndEmpresaId(orden.getVentaId(), orden.getEmpresaId()).orElse(null);
        TerminalMercadoPagoEntidad terminal = terminales.findByIdAndEmpresaId(orden.getTerminalId(), orden.getEmpresaId()).orElse(null);
        java.math.BigDecimal efectivo=null;boolean planInvalido=false;boolean reembolsable=false;
        try {var partes=previstos.leer(orden.getPagosPrevistosJson());reembolsable=partes.isEmpty() && "APROBADO".equals(orden.getEstado()) && (orden.getDetalleEstado()==null || !orden.getDetalleEstado().startsWith("REVISAR:"));efectivo=partes.stream().filter(p -> com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.METODO_PAGO_EFECTIVO.equals(p.metodoPago())).map(com.techprotech.agenda.modulos.caja.aplicacion.PlanPagosVenta.Parte::monto).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);}
        catch(IllegalStateException ex){planInvalido=true;}
        return new OrdenPointResponse(orden.getPublicId(), orden.getVentaId(), venta == null ? null : venta.getFolio(), orden.getTerminalId(),
                terminal == null ? null : terminal.getNombre(), orden.getEstado(), orden.getDetalleEstado(), orden.getImporte(), orden.getMoneda(),
                orden.getProveedorOrdenId(), orden.getProveedorPagoId(), orden.getCreadoEn(), orden.getActualizadoEn(), simulacionDisponible(terminal),
                efectivo,false,false,orden.getDetalleEstado()!=null && orden.getDetalleEstado().startsWith("REVISAR:"),planInvalido,reembolsable);
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
