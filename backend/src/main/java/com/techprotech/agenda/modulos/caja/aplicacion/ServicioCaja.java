package com.techprotech.agenda.modulos.caja.aplicacion;

import static com.techprotech.agenda.compartido.tiempo.MomentoUtc.de;

import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.caja.api.dto.AbrirCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.CatalogoCajaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.CajaSesionResponse;
import com.techprotech.agenda.modulos.caja.api.dto.CerrarCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.CitaPorCobrarResponse;
import com.techprotech.agenda.modulos.caja.api.dto.MovimientoCajaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.PagoCitaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarMovimientoCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarPagoRequest;
import com.techprotech.agenda.modulos.caja.api.dto.ResumenCajaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.SucursalCajaCatalogoResponse;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.CajaSesionEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.MovimientoCajaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.PagoCitaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.PagoVentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.CajaSesionRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.MovimientoCajaRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoCitaRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoVentaRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaDetalleRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.*;

@Service
public class ServicioCaja {
    private final java.time.Clock reloj;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServicioCaja.class);

    private final CajaSesionRepositorio cajaSesionRepositorio;
    private final PagoCitaRepositorio pagoCitaRepositorio;
    private final PagoVentaRepositorio pagoVentaRepositorio;
    private final VentaDetalleRepositorio ventaDetalleRepositorio;
    private final MovimientoCajaRepositorio movimientoCajaRepositorio;
    private final CitaRepositorio citaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    public ServicioCaja(
            CajaSesionRepositorio cajaSesionRepositorio,
            PagoCitaRepositorio pagoCitaRepositorio,
            PagoVentaRepositorio pagoVentaRepositorio,
            VentaDetalleRepositorio ventaDetalleRepositorio,
            MovimientoCajaRepositorio movimientoCajaRepositorio,
            CitaRepositorio citaRepositorio,
            ClienteRepositorio clienteRepositorio,
            ServicioRepositorio servicioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            com.fasterxml.jackson.databind.ObjectMapper json,
            java.time.Clock reloj
    ) {
        this.reloj = reloj;
        this.cajaSesionRepositorio = cajaSesionRepositorio;
        this.pagoCitaRepositorio = pagoCitaRepositorio;
        this.pagoVentaRepositorio = pagoVentaRepositorio;
        this.ventaDetalleRepositorio = ventaDetalleRepositorio;
        this.movimientoCajaRepositorio = movimientoCajaRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public CatalogoCajaResponse catalogo(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
        if (sucursalId != null) {
            validarAccesoSucursal(sucursalesPermitidas, sucursalId);
        }

        List<SucursalEntidad> sucursalesActivas =
                sucursalRepositorio.findByEmpresaIdAndActivaTrue(empresaId).stream()
                        .filter(sucursal -> !tieneScopeSucursales(sucursalesPermitidas) || sucursalesPermitidas.contains(sucursal.getId()))
                        .toList();

        List<SucursalEntidad> sucursalesDisponibles =
                !sucursalesActivas.isEmpty()
                        ? sucursalesActivas
                        : sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                        .filter(sucursal -> !tieneScopeSucursales(sucursalesPermitidas) || sucursalesPermitidas.contains(sucursal.getId()))
                        .toList();

        Long sucursalOperativaId = sucursalId != null
                ? sucursalId
                : sucursalesPermitidas != null && !sucursalesPermitidas.isEmpty()
                ? sucursalesPermitidas.getFirst()
                : sucursalesDisponibles.stream().findFirst().map(SucursalEntidad::getId).orElse(null);

        return new CatalogoCajaResponse(
                sucursalOperativaId,
                sucursalesDisponibles.stream()
                        .map(sucursal -> new SucursalCajaCatalogoResponse(
                                sucursal.getId(),
                                sucursal.getEmpresaId(),
                                sucursal.getNombre(),
                                sucursal.getDireccion(),
                                sucursal.getTelefono(),
                                sucursal.getZonaHoraria()
                        ))
                        .toList(),
                METODOS_PAGO.stream().map(opcion -> new CatalogoCajaResponse.OpcionCajaResponse(opcion.codigo(), opcion.etiqueta())).toList(),
                TIPOS_MOVIMIENTO_MANUAL.stream().map(opcion -> new CatalogoCajaResponse.OpcionCajaResponse(opcion.codigo(), opcion.etiqueta())).toList(),
                ESTADOS_SESION.stream().map(opcion -> new CatalogoCajaResponse.OpcionCajaResponse(opcion.codigo(), opcion.etiqueta())).toList(),
                ESTADO_SESION_ABIERTA,
                ESTADO_SESION_CERRADA,
                METODO_PAGO_EFECTIVO
        );
    }

    @Transactional
    public CajaSesionResponse abrirSesion(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, AbrirCajaRequest request) {
        validarAccesoSucursal(sucursalesPermitidas, request.sucursalId());
        // Bloqueo pesimista: evita que dos hilos concurrentes abran dos sesiones para la misma sucursal
        cajaSesionRepositorio.findParaAperturaConBloqueo(empresaId, request.sucursalId(), ESTADO_SESION_ABIERTA)
                .ifPresent(sesion -> {
                    throw new ResponseStatusException(CONFLICT, "Ya existe una caja abierta para esa sucursal");
                });

        CajaSesionEntidad sesion = new CajaSesionEntidad();
        sesion.setEmpresaId(empresaId);
        sesion.setSucursalId(request.sucursalId());
        sesion.setEstado(ESTADO_SESION_ABIERTA);
        sesion.setMontoInicial(request.montoInicial());
        sesion.setMontoEsperado(request.montoInicial());
        sesion.setObservaciones(normalizarOpcional(request.observaciones()));
        sesion.setAbiertaPorUsuarioId(usuarioId);
        sesion.setAbiertaEn(LocalDateTime.now(reloj));
        return mapearSesion(cajaSesionRepositorio.save(sesion));
    }

    @Transactional
    public CajaSesionResponse cerrarSesion(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, Long cajaSesionId, CerrarCajaRequest request) {
        CajaSesionEntidad sesion = cajaSesionRepositorio.findByIdAndEmpresaId(cajaSesionId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sesión de caja no existe para la empresa"));
        validarAccesoSucursal(sucursalesPermitidas, sesion.getSucursalId());
        if (!ESTADO_SESION_ABIERTA.equalsIgnoreCase(sesion.getEstado())) {
            throw new ResponseStatusException(CONFLICT, "La caja indicada ya está cerrada");
        }

        BigDecimal esperado = calcularSaldoEsperado(sesion);
        sesion.setMontoEsperado(esperado);
        sesion.setMontoContado(request.montoContado());
        sesion.setDiferencia(request.montoContado().subtract(esperado));
        sesion.setEstado(ESTADO_SESION_CERRADA);
        sesion.setCerradaPorUsuarioId(usuarioId);
        sesion.setCerradaEn(LocalDateTime.now(reloj));
        if (normalizarOpcional(request.observaciones()) != null) {
            sesion.setObservaciones(normalizarOpcional(request.observaciones()));
        }
        return mapearSesion(cajaSesionRepositorio.save(sesion));
    }

    @Transactional(readOnly = true)
    public CajaSesionResponse obtenerSesionActual(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
        Long sucursalConsulta = resolverSucursalConsulta(sucursalesPermitidas, sucursalId);
        return resolverSesionAbierta(empresaId, sucursalConsulta)
                .map(this::mapearSesion)
                .orElse(null);
    }

    /**
     * Lista citas cobrables del día con batch-loading de datos relacionados.
     * Antes: 1 + N*5 queries (250+ para 50 citas). Ahora: ~7 queries fijas.
     */
    @Transactional(readOnly = true)
    public List<CitaPorCobrarResponse> listarCitasPorCobrar(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
        Long sucursalConsulta = resolverSucursalConsulta(sucursalesPermitidas, sucursalId);

        // Query 1: citas candidatas filtradas por estado
        List<CitaEntidad> candidatas = citaRepositorio.findByEmpresaIdAndEstadoInOrderByInicioDesc(
                empresaId, List.of("PENDIENTE", "CONFIRMADA", "FINALIZADA"));

        // Query 2: zona horaria de sucursales en batch
        Set<Long> sucursalIds = candidatas.stream().map(CitaEntidad::getSucursalId).collect(Collectors.toSet());
        Map<Long, String> zonaHorariaPorSucursal = sucursalRepositorio.findAllById(sucursalIds).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, s -> s.getZonaHoraria() != null ? s.getZonaHoraria() : "America/Mexico_City"));

        // Filtros en memoria (sin queries adicionales)
        List<CitaEntidad> cobrable = candidatas.stream()
                .filter(cita -> sucursalConsulta == null || sucursalConsulta.equals(cita.getSucursalId()))
                .filter(cita -> !tieneScopeSucursales(sucursalesPermitidas) || sucursalesPermitidas.contains(cita.getSucursalId()))
                .filter(cita -> esDelDiaOperativo(cita, zonaHorariaPorSucursal))
                .filter(this::esCitaCobrable)
                .toList();

        if (cobrable.isEmpty()) return List.of();

        Set<Long> citaIds = cobrable.stream().map(CitaEntidad::getId).collect(Collectors.toSet());
        Set<Long> clienteIds = cobrable.stream().map(CitaEntidad::getClienteId).collect(Collectors.toSet());
        Set<Long> servicioIds = cobrable.stream().map(CitaEntidad::getServicioId).collect(Collectors.toSet());

        // Query 3: clientes en batch
        Map<Long, ClienteEntidad> clientes = clienteRepositorio.findAllById(clienteIds).stream()
                .collect(Collectors.toMap(ClienteEntidad::getUsuarioId, Function.identity()));

        // Query 4: servicios en batch
        Map<Long, String> nombreServicio = servicioRepositorio.findAllById(servicioIds).stream()
                .collect(Collectors.toMap(ServicioEntidad::getId, ServicioEntidad::getNombre));

        // Query 5: sucursal nombres (reutilizamos los ya cargados desde sucursalIds)
        Map<Long, String> nombreSucursal = sucursalRepositorio.findAllById(sucursalIds).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));

        // Query 6: suma de pagos de cita en batch
        Map<Long, BigDecimal> pagadoCita = pagosEnBatch(pagoCitaRepositorio.sumarPagadoPorCitas(empresaId, citaIds));

        // Query 7: suma de ventas pagadas por cita en batch
        Map<Long, BigDecimal> pagadoVenta = pagosEnBatch(ventaDetalleRepositorio.sumarServiciosPagadosPorCitas(empresaId, citaIds));

        return cobrable.stream()
                .map(cita -> {
                    ClienteEntidad cliente = clientes.get(cita.getClienteId());
                    BigDecimal pagado = montoSeguro(pagadoCita.get(cita.getId()))
                            .add(montoSeguro(pagadoVenta.get(cita.getId())));
                    BigDecimal pendiente = cita.getPrecio().subtract(pagado).max(BigDecimal.ZERO);
                    return new CitaPorCobrarResponse(
                            cita.getId(),
                            cliente != null ? cliente.getNombreCompleto() : "Cliente",
                            cliente != null ? cliente.getTelefono() : null,
                            nombreServicio.getOrDefault(cita.getServicioId(), "Servicio"),
                            nombreSucursal.getOrDefault(cita.getSucursalId(), "Sucursal"),
                            cita.getInicio(),
                            cita.getPrecio(),
                            pagado,
                            pendiente,
                            cita.getMoneda(),
                            cita.getEstado(),
                            pendiente.compareTo(BigDecimal.ZERO) <= 0 ? "PAGADO"
                                    : (pagado.compareTo(BigDecimal.ZERO) > 0 ? "PARCIAL" : "PENDIENTE"),
                            cita.getPrecioLista() != null ? cita.getPrecioLista() : cita.getPrecio(),
                            montoSeguro(cita.getDescuento()),
                            tituloPromocion(cita)
                    );
                })
                .filter(cita -> cita.pendiente().compareTo(BigDecimal.ZERO) > 0)
                .sorted(java.util.Comparator.comparing(CitaPorCobrarResponse::inicio).reversed())
                .toList();
    }

    private String tituloPromocion(CitaEntidad cita) {
        if (cita.getPromocionSnapshotJson() == null) return null;
        try {
            return json.readTree(cita.getPromocionSnapshotJson()).path("titulo").asText(null);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            // El título es informativo: conservar el cobro y no registrar el contenido del snapshot.
            log.warn("Snapshot de promoción inválido en cita {} de empresa {}; se omite el título",
                    cita.getId(), cita.getEmpresaId());
            return null;
        }
    }

    @Transactional
    public PagoCitaResponse registrarPago
    (Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, Long citaId, RegistrarPagoRequest request) {
            String metodoPago = normalizarMayusculas(request.metodoPago());
            if (!contiene(METODOS_PAGO, metodoPago)) {
                throw new ResponseStatusException(BAD_REQUEST, "El método de pago no es válido");
            }
            CitaEntidad cita = citaRepositorio.findByIdAndEmpresaId(citaId, empresaId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa"));
            validarAccesoSucursal(sucursalesPermitidas, cita.getSucursalId());
            if (!esCitaCobrable(cita)) {
                throw new ResponseStatusException(CONFLICT, "La cita solo se puede cobrar después del check-in o cuando haya sido finalizada");
            }

            BigDecimal pagado = totalPagadoCita(empresaId, citaId);
            BigDecimal pendiente = cita.getPrecio().subtract(pagado);
            if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(CONFLICT, "La cita ya está pagada");
            }
            if (request.monto().compareTo(pendiente) > 0) {
                throw new ResponseStatusException(CONFLICT, "El pago supera el saldo pendiente de la cita");
            }

            CajaSesionEntidad sesion = obtenerSesionAbiertaObligatoria(empresaId, cita.getSucursalId());
            PagoCitaEntidad pago = new PagoCitaEntidad();
            pago.setEmpresaId(empresaId);
            pago.setCitaId(citaId);
            pago.setCajaSesionId(sesion.getId());
            pago.setMonto(request.monto());
            pago.setMetodoPago(metodoPago);
            pago.setReferencia(normalizarOpcional(request.referencia()));
            pago.setObservaciones(normalizarOpcional(request.observaciones()));
            pago.setRegistradoPorUsuarioId(usuarioId);
            pago.setRegistradoEn(LocalDateTime.now(reloj));
            PagoCitaEntidad guardado = pagoCitaRepositorio.save(pago);

            if (METODO_PAGO_EFECTIVO.equals(guardado.getMetodoPago())) {
                registrarMovimientoInterno(
                        empresaId, sesion.getId(), citaId,
                        "INGRESO_CITA", METODO_PAGO_EFECTIVO, guardado.getMonto(),
                        "Cobro de cita #" + citaId,
                        guardado.getReferencia(), guardado.getObservaciones(), usuarioId
                );
            }

            return mapearPago(guardado);
        }

        @Transactional(readOnly = true)
        public List<PagoCitaResponse> listarPagosCita(Long empresaId, List<Long> sucursalesPermitidas, Long citaId) {
            CitaEntidad cita = citaRepositorio.findByIdAndEmpresaId(citaId, empresaId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa"));
            validarAccesoSucursal(sucursalesPermitidas, cita.getSucursalId());
            List<PagoCitaResponse> resultado = new ArrayList<>(pagoCitaRepositorio
                    .findByEmpresaIdAndCitaIdOrderByRegistradoEnDesc(empresaId, citaId).stream()
                    .map(this::mapearPago)
                    .toList());
            resultado.addAll(pagoVentaRepositorio.findPagosConciliadosPorCita(empresaId, citaId).stream()
                    .map(p -> new PagoCitaResponse(p.id(), p.citaId(), p.cajaSesionId(), p.monto(),
                            p.metodoPago(), p.referencia(), p.observaciones(), de(p.registradoEn())))
                    .toList());
            resultado.sort(java.util.Comparator.comparing(PagoCitaResponse::registradoEn,
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
            return resultado;
        }

        @Transactional
        public MovimientoCajaResponse registrarMovimiento(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, RegistrarMovimientoCajaRequest request) {
            String tipoMovimiento = normalizarMayusculas(request.tipoMovimiento());
            String metodoPago = normalizarOpcionalMayusculas(request.metodoPago());
            if (!contiene(TIPOS_MOVIMIENTO_MANUAL, tipoMovimiento)) {
                throw new ResponseStatusException(BAD_REQUEST, "El tipo de movimiento no es válido");
            }
            if (metodoPago != null && !contiene(METODOS_PAGO, metodoPago)) {
                throw new ResponseStatusException(BAD_REQUEST, "El método de pago no es válido");
            }
            validarAccesoSucursal(sucursalesPermitidas, request.sucursalId());
            CajaSesionEntidad sesion = obtenerSesionAbiertaObligatoria(empresaId, request.sucursalId());
            MovimientoCajaEntidad movimiento = registrarMovimientoInterno(
                    empresaId, sesion.getId(), null,
                    tipoMovimiento, metodoPago, request.monto(),
                    request.concepto().trim(),
                    normalizarOpcional(request.referencia()),
                    normalizarOpcional(request.observaciones()),
                    usuarioId
            );
            return mapearMovimiento(movimiento);
        }

        @Transactional(readOnly = true)
        public List<MovimientoCajaResponse> listarMovimientos(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
            Long sucursalConsulta = resolverSucursalConsulta(sucursalesPermitidas, sucursalId);
            CajaSesionEntidad sesion = resolverSesionAbierta(empresaId, sucursalConsulta).orElse(null);
            if (sesion == null) {
                return List.of();
            }
            return movimientoCajaRepositorio.findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(empresaId, sesion.getId()).stream()
                    .map(this::mapearMovimiento)
                    .toList();
        }

        /**
         * Resumen del turno. Optimizado para:
         * 1. No hacer N+1 al contar citas (reutiliza listarCitasPorCobrar ya optimizado).
         * 2. Una sola pasada para calcular totales por método de pago.
         * 3. No recargar movimientos que ya están en memoria.
         */
        @Transactional(readOnly = true)
        public ResumenCajaResponse resumen(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
            Long sucursalConsulta = resolverSucursalConsulta(sucursalesPermitidas, sucursalId);

            // listarCitasPorCobrar ya usa batch-loading (~7 queries) — no N+1
            int citasPendientes = listarCitasPorCobrar(empresaId, sucursalesPermitidas, sucursalConsulta).size();

            CajaSesionEntidad sesion = resolverSesionAbierta(empresaId, sucursalConsulta).orElse(null);
            if (sesion == null) {
                return new ResumenCajaResponse(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, citasPendientes);
            }

            List<PagoCitaEntidad> pagos = pagoCitaRepositorio.findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(empresaId, sesion.getId());
            List<PagoVentaEntidad> pagosVenta = pagoVentaRepositorio.findPagosVigentesDeSesion(empresaId, sesion.getId());
            List<MovimientoCajaEntidad> movimientos = movimientoCajaRepositorio.findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(empresaId, sesion.getId());

            // Una sola pasada sobre cada lista para acumular totales por método de pago
            Map<String, BigDecimal> totalesPagoCita = new HashMap<>();
            for (PagoCitaEntidad p : pagos) {
                totalesPagoCita.merge(p.getMetodoPago(), p.getMonto(), BigDecimal::add);
            }
            Map<String, BigDecimal> totalesPagoVenta = new HashMap<>();
            for (PagoVentaEntidad p : pagosVenta) {
                totalesPagoVenta.merge(p.getMetodoPago(), p.getMonto(), BigDecimal::add);
            }

            BigDecimal totalCobrado = sumar(totalesPagoCita.values()).add(sumar(totalesPagoVenta.values()));
            BigDecimal totalEfectivo = montoSeguro(totalesPagoCita.get("EFECTIVO")).add(montoSeguro(totalesPagoVenta.get("EFECTIVO")));
            BigDecimal totalTarjeta = montoSeguro(totalesPagoCita.get("TARJETA")).add(montoSeguro(totalesPagoVenta.get("TARJETA")));
            BigDecimal totalPoint = montoSeguro(totalesPagoVenta.get("MERCADO_PAGO_POINT"));
            BigDecimal totalTransferencia = montoSeguro(totalesPagoCita.get("TRANSFERENCIA")).add(montoSeguro(totalesPagoVenta.get("TRANSFERENCIA")));

            BigDecimal totalGastos = BigDecimal.ZERO;
            BigDecimal totalRetiros = BigDecimal.ZERO;
            for (MovimientoCajaEntidad m : movimientos) {
                switch (m.getTipoMovimiento()) {
                    case "GASTO_MENOR", "AJUSTE_NEGATIVO" -> totalGastos = totalGastos.add(m.getMonto());
                    case "RETIRO" -> totalRetiros = totalRetiros.add(m.getMonto());
                    default -> { }
                }
            }

            // Reutiliza los movimientos ya cargados — no hace query adicional
            BigDecimal saldoEsperado = calcularSaldoEsperado(sesion, movimientos);

            return new ResumenCajaResponse(
                    mapearSesionConEsperado(sesion, saldoEsperado),
                    totalCobrado, totalEfectivo, totalTarjeta, totalPoint,
                    totalTransferencia, totalGastos, totalRetiros, saldoEsperado,
                    citasPendientes
            );
        }

        // ── Helpers privados ─────────────────────────────────────────────────────

        private boolean tieneScopeSucursales(List<Long> sucursalesPermitidas) {
            return sucursalesPermitidas != null && !sucursalesPermitidas.isEmpty();
        }

        private void validarAccesoSucursal(List<Long> sucursalesPermitidas, Long sucursalId) {
            if (tieneScopeSucursales(sucursalesPermitidas) && (sucursalId == null || !sucursalesPermitidas.contains(sucursalId))) {
                throw new ResponseStatusException(FORBIDDEN, "No tienes acceso a la sucursal indicada");
            }
        }

        private Long resolverSucursalConsulta(List<Long> sucursalesPermitidas, Long sucursalId) {
            if (sucursalId != null) {
                validarAccesoSucursal(sucursalesPermitidas, sucursalId);
                return sucursalId;
            }
            if (tieneScopeSucursales(sucursalesPermitidas) && sucursalesPermitidas.size() == 1) {
                return sucursalesPermitidas.getFirst();
            }
            return null;
        }

        private Optional<CajaSesionEntidad> resolverSesionAbierta(Long empresaId, Long sucursalId) {
            if (sucursalId != null) {
                return cajaSesionRepositorio.findByEmpresaIdAndSucursalIdAndEstado(empresaId, sucursalId, ESTADO_SESION_ABIERTA);
            }
            return cajaSesionRepositorio.findFirstByEmpresaIdAndEstadoOrderByAbiertaEnDesc(empresaId, ESTADO_SESION_ABIERTA);
        }

        private CajaSesionEntidad obtenerSesionAbiertaObligatoria(Long empresaId, Long sucursalId) {
            return resolverSesionAbierta(empresaId, sucursalId)
                    .orElseThrow(() -> new ResponseStatusException(CONFLICT, "No hay una caja abierta para la sucursal indicada"));
        }

        private MovimientoCajaEntidad registrarMovimientoInterno(
                Long empresaId, Long cajaSesionId, Long citaId, String tipoMovimiento,
                String metodoPago, BigDecimal monto, String concepto,
                String referencia, String observaciones, Long usuarioId
        ) {
            MovimientoCajaEntidad movimiento = new MovimientoCajaEntidad();
            movimiento.setEmpresaId(empresaId);
            movimiento.setCajaSesionId(cajaSesionId);
            movimiento.setCitaId(citaId);
            movimiento.setTipoMovimiento(tipoMovimiento);
            movimiento.setMetodoPago(metodoPago);
            movimiento.setMonto(monto);
            movimiento.setConcepto(concepto);
            movimiento.setReferencia(referencia);
            movimiento.setObservaciones(observaciones);
            movimiento.setRegistradoPorUsuarioId(usuarioId);
            movimiento.setRegistradoEn(LocalDateTime.now(reloj));
            return movimientoCajaRepositorio.save(movimiento);
        }

        /** Calcula saldo esperado cargando movimientos desde BD (para cerrar sesión). */
        private BigDecimal calcularSaldoEsperado(CajaSesionEntidad sesion) {
            List<MovimientoCajaEntidad> movimientos = movimientoCajaRepositorio
                    .findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(sesion.getEmpresaId(), sesion.getId());
            return calcularSaldoEsperado(sesion, movimientos);
        }

        /** Calcula saldo esperado a partir de una lista ya cargada — evita query redundante. */
        private BigDecimal calcularSaldoEsperado(CajaSesionEntidad sesion, List<MovimientoCajaEntidad> movimientos) {
            BigDecimal saldo = montoSeguro(sesion.getMontoInicial());
            for (MovimientoCajaEntidad movimiento : movimientos) {
                switch (movimiento.getTipoMovimiento()) {
                    case "INGRESO_CITA", "INGRESO_VENTA", "INGRESO_EXTRA", "AJUSTE_POSITIVO" -> saldo = saldo.add(movimiento.getMonto());
                    case "GASTO_MENOR", "RETIRO", "AJUSTE_NEGATIVO" -> saldo = saldo.subtract(movimiento.getMonto());
                    default -> { }
                }
            }
            return saldo;
        }

        private boolean esCitaCobrable(CitaEntidad cita) {
            if (cita == null) return false;
            if (List.of("CANCELADA", "NO_ASISTIO").contains(cita.getEstado())) return false;
            return cita.getCheckInEn() != null || "FINALIZADA".equalsIgnoreCase(cita.getEstado());
        }

        /** Recibe el mapa de zona horaria pre-cargado para evitar N+1. */
        private boolean esDelDiaOperativo(CitaEntidad cita, Map<Long, String> zonaHorariaPorSucursal) {
            String zonaHoraria = zonaHorariaPorSucursal.getOrDefault(cita.getSucursalId(), "America/Mexico_City");
            ZoneId zoneId = ZoneId.of(zonaHoraria);
            LocalDate hoy = LocalDate.now(reloj.withZone(zoneId));
            return cita.getInicio().atZone(zoneId).toLocalDate().equals(hoy);
        }

        private CajaSesionResponse mapearSesion(CajaSesionEntidad sesion) {
            return new CajaSesionResponse(
                    sesion.getId(),
                    sesion.getSucursalId(),
                    sucursalRepositorio.findById(sesion.getSucursalId()).map(SucursalEntidad::getNombre).orElse("Sucursal"),
                    sesion.getEstado(),
                    sesion.getMontoInicial(),
                    sesion.getMontoEsperado(),
                    sesion.getMontoContado(),
                    sesion.getDiferencia(),
                    sesion.getObservaciones(),
                    de(sesion.getAbiertaEn()),
                    de(sesion.getCerradaEn())
            );
        }

        /** Evita cargar movimientos de nuevo cuando el llamador ya los tiene. */
        private CajaSesionResponse mapearSesionConEsperado(CajaSesionEntidad sesion, BigDecimal saldoEsperado) {
            sesion.setMontoEsperado(saldoEsperado);
            return mapearSesion(sesion);
        }

        private PagoCitaResponse mapearPago(PagoCitaEntidad pago) {
            return new PagoCitaResponse(
                    pago.getId(),
                    pago.getCitaId(),
                    pago.getCajaSesionId(),
                    pago.getMonto(),
                    pago.getMetodoPago(),
                    pago.getReferencia(),
                    pago.getObservaciones(),
                    de(pago.getRegistradoEn())
            );
        }

        private BigDecimal totalPagadoCita(Long empresaId, Long citaId) {
            return montoSeguro(pagoCitaRepositorio.sumarPagadoPorCita(empresaId, citaId))
                    .add(montoSeguro(ventaDetalleRepositorio.sumarServiciosPagadosPorCita(empresaId, citaId)));
        }

        private MovimientoCajaResponse mapearMovimiento(MovimientoCajaEntidad movimiento) {
            return new MovimientoCajaResponse(
                    movimiento.getId(),
                    movimiento.getCajaSesionId(),
                    movimiento.getCitaId(),
                    movimiento.getTipoMovimiento(),
                    movimiento.getMetodoPago(),
                    movimiento.getMonto(),
                    movimiento.getConcepto(),
                    movimiento.getReferencia(),
                    movimiento.getObservaciones(),
                    de(movimiento.getRegistradoEn())
            );
        }

        /** Convierte filas [citaId, total] de queries batch en Map. */
        private Map<Long, BigDecimal> pagosEnBatch(List<Object[]> filas) {
            Map<Long, BigDecimal> mapa = new HashMap<>();
            for (Object[] fila : filas) {
                mapa.put(((Number) fila[0]).longValue(), (BigDecimal) fila[1]);
            }
            return mapa;
        }

        private BigDecimal sumar(Collection<BigDecimal> valores) {
            return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal montoSeguro(BigDecimal valor) {
            return valor != null ? valor : BigDecimal.ZERO;
        }

        private String normalizarOpcional(String valor) {
            return valor == null || valor.isBlank() ? null : valor.trim();
        }

        private String normalizarMayusculas(String valor) {
            return valor == null ? null : valor.trim().toUpperCase(Locale.ROOT);
        }

        private String normalizarOpcionalMayusculas(String valor) {
            String normalizado = normalizarOpcional(valor);
            return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT);
        }
    }
