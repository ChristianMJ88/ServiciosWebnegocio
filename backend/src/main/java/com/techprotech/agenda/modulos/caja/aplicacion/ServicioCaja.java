package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.caja.api.dto.AbrirCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.CajaSesionResponse;
import com.techprotech.agenda.modulos.caja.api.dto.CerrarCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.CitaPorCobrarResponse;
import com.techprotech.agenda.modulos.caja.api.dto.MovimientoCajaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.PagoCitaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarMovimientoCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarPagoRequest;
import com.techprotech.agenda.modulos.caja.api.dto.ResumenCajaResponse;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.CajaSesionEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.MovimientoCajaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.PagoCitaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.CajaSesionRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.MovimientoCajaRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoCitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioCaja {

    private static final String ESTADO_ABIERTA = "ABIERTA";
    private static final String ESTADO_CERRADA = "CERRADA";

    private final CajaSesionRepositorio cajaSesionRepositorio;
    private final PagoCitaRepositorio pagoCitaRepositorio;
    private final MovimientoCajaRepositorio movimientoCajaRepositorio;
    private final CitaRepositorio citaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;

    public ServicioCaja(
            CajaSesionRepositorio cajaSesionRepositorio,
            PagoCitaRepositorio pagoCitaRepositorio,
            MovimientoCajaRepositorio movimientoCajaRepositorio,
            CitaRepositorio citaRepositorio,
            ClienteRepositorio clienteRepositorio,
            ServicioRepositorio servicioRepositorio,
            SucursalRepositorio sucursalRepositorio
    ) {
        this.cajaSesionRepositorio = cajaSesionRepositorio;
        this.pagoCitaRepositorio = pagoCitaRepositorio;
        this.movimientoCajaRepositorio = movimientoCajaRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
    }

    @Transactional
    public CajaSesionResponse abrirSesion(Long empresaId, Long usuarioId, AbrirCajaRequest request) {
        cajaSesionRepositorio.findByEmpresaIdAndSucursalIdAndEstado(empresaId, request.sucursalId(), ESTADO_ABIERTA)
                .ifPresent(sesion -> {
                    throw new ResponseStatusException(CONFLICT, "Ya existe una caja abierta para esa sucursal");
                });

        CajaSesionEntidad sesion = new CajaSesionEntidad();
        sesion.setEmpresaId(empresaId);
        sesion.setSucursalId(request.sucursalId());
        sesion.setEstado(ESTADO_ABIERTA);
        sesion.setMontoInicial(request.montoInicial());
        sesion.setMontoEsperado(request.montoInicial());
        sesion.setObservaciones(normalizarOpcional(request.observaciones()));
        sesion.setAbiertaPorUsuarioId(usuarioId);
        sesion.setAbiertaEn(LocalDateTime.now());
        return mapearSesion(cajaSesionRepositorio.save(sesion));
    }

    @Transactional
    public CajaSesionResponse cerrarSesion(Long empresaId, Long usuarioId, Long cajaSesionId, CerrarCajaRequest request) {
        CajaSesionEntidad sesion = cajaSesionRepositorio.findByIdAndEmpresaId(cajaSesionId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sesión de caja no existe para la empresa"));
        if (!ESTADO_ABIERTA.equalsIgnoreCase(sesion.getEstado())) {
            throw new ResponseStatusException(CONFLICT, "La caja indicada ya está cerrada");
        }

        BigDecimal esperado = calcularSaldoEsperado(sesion);
        sesion.setMontoEsperado(esperado);
        sesion.setMontoContado(request.montoContado());
        sesion.setDiferencia(request.montoContado().subtract(esperado));
        sesion.setEstado(ESTADO_CERRADA);
        sesion.setCerradaPorUsuarioId(usuarioId);
        sesion.setCerradaEn(LocalDateTime.now());
        if (normalizarOpcional(request.observaciones()) != null) {
            sesion.setObservaciones(normalizarOpcional(request.observaciones()));
        }
        return mapearSesion(cajaSesionRepositorio.save(sesion));
    }

    @Transactional(readOnly = true)
    public CajaSesionResponse obtenerSesionActual(Long empresaId, Long sucursalId) {
        return resolverSesionAbierta(empresaId, sucursalId)
                .map(this::mapearSesion)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CitaPorCobrarResponse> listarCitasPorCobrar(Long empresaId, Long sucursalId) {
        return citaRepositorio.findByEmpresaIdAndEstadoInOrderByInicioDesc(empresaId, List.of("FINALIZADA")).stream()
                .filter(cita -> sucursalId == null || sucursalId.equals(cita.getSucursalId()))
                .map(this::mapearCitaPorCobrar)
                .filter(cita -> cita.pendiente().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(CitaPorCobrarResponse::inicio).reversed())
                .toList();
    }

    @Transactional
    public PagoCitaResponse registrarPago(Long empresaId, Long usuarioId, Long citaId, RegistrarPagoRequest request) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaId(citaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa"));
        if (!"FINALIZADA".equalsIgnoreCase(cita.getEstado())) {
            throw new ResponseStatusException(CONFLICT, "Solo se pueden cobrar citas finalizadas");
        }

        BigDecimal pagado = montoSeguro(pagoCitaRepositorio.sumarPagadoPorCita(empresaId, citaId));
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
        pago.setMetodoPago(normalizarMayusculas(request.metodoPago()));
        pago.setReferencia(normalizarOpcional(request.referencia()));
        pago.setObservaciones(normalizarOpcional(request.observaciones()));
        pago.setRegistradoPorUsuarioId(usuarioId);
        pago.setRegistradoEn(LocalDateTime.now());
        PagoCitaEntidad guardado = pagoCitaRepositorio.save(pago);

        if ("EFECTIVO".equals(guardado.getMetodoPago())) {
            registrarMovimientoInterno(
                    empresaId,
                    sesion.getId(),
                    citaId,
                    "INGRESO_CITA",
                    "EFECTIVO",
                    guardado.getMonto(),
                    "Cobro de cita #" + citaId,
                    guardado.getReferencia(),
                    guardado.getObservaciones(),
                    usuarioId
            );
        }

        return mapearPago(guardado);
    }

    @Transactional(readOnly = true)
    public List<PagoCitaResponse> listarPagosCita(Long empresaId, Long citaId) {
        citaRepositorio.findByIdAndEmpresaId(citaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa"));
        return pagoCitaRepositorio.findByEmpresaIdAndCitaIdOrderByRegistradoEnDesc(empresaId, citaId).stream()
                .map(this::mapearPago)
                .toList();
    }

    @Transactional
    public MovimientoCajaResponse registrarMovimiento(Long empresaId, Long usuarioId, RegistrarMovimientoCajaRequest request) {
        CajaSesionEntidad sesion = obtenerSesionAbiertaObligatoria(empresaId, request.sucursalId());
        MovimientoCajaEntidad movimiento = registrarMovimientoInterno(
                empresaId,
                sesion.getId(),
                null,
                normalizarMayusculas(request.tipoMovimiento()),
                normalizarOpcionalMayusculas(request.metodoPago()),
                request.monto(),
                request.concepto().trim(),
                normalizarOpcional(request.referencia()),
                normalizarOpcional(request.observaciones()),
                usuarioId
        );
        return mapearMovimiento(movimiento);
    }

    @Transactional(readOnly = true)
    public ResumenCajaResponse resumen(Long empresaId, Long sucursalId) {
        CajaSesionEntidad sesion = resolverSesionAbierta(empresaId, sucursalId).orElse(null);
        if (sesion == null) {
            return new ResumenCajaResponse(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        List<PagoCitaEntidad> pagos = pagoCitaRepositorio.findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(empresaId, sesion.getId());
        List<MovimientoCajaEntidad> movimientos = movimientoCajaRepositorio.findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(empresaId, sesion.getId());

        BigDecimal totalCobrado = pagos.stream().map(PagoCitaEntidad::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEfectivo = pagos.stream().filter(p -> "EFECTIVO".equals(p.getMetodoPago())).map(PagoCitaEntidad::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTarjeta = pagos.stream().filter(p -> "TARJETA".equals(p.getMetodoPago())).map(PagoCitaEntidad::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTransferencia = pagos.stream().filter(p -> "TRANSFERENCIA".equals(p.getMetodoPago())).map(PagoCitaEntidad::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGastos = movimientos.stream()
                .filter(m -> List.of("GASTO_MENOR", "AJUSTE_NEGATIVO").contains(m.getTipoMovimiento()))
                .map(MovimientoCajaEntidad::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRetiros = movimientos.stream()
                .filter(m -> "RETIRO".equals(m.getTipoMovimiento()))
                .map(MovimientoCajaEntidad::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumenCajaResponse(
                mapearSesionConEsperado(sesion),
                totalCobrado,
                totalEfectivo,
                totalTarjeta,
                totalTransferencia,
                totalGastos,
                totalRetiros,
                calcularSaldoEsperado(sesion),
                listarCitasPorCobrar(empresaId, sucursalId).size()
        );
    }

    private java.util.Optional<CajaSesionEntidad> resolverSesionAbierta(Long empresaId, Long sucursalId) {
        if (sucursalId != null) {
            return cajaSesionRepositorio.findByEmpresaIdAndSucursalIdAndEstado(empresaId, sucursalId, ESTADO_ABIERTA);
        }
        return cajaSesionRepositorio.findFirstByEmpresaIdAndEstadoOrderByAbiertaEnDesc(empresaId, ESTADO_ABIERTA);
    }

    private CajaSesionEntidad obtenerSesionAbiertaObligatoria(Long empresaId, Long sucursalId) {
        return resolverSesionAbierta(empresaId, sucursalId)
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "No hay una caja abierta para la sucursal indicada"));
    }

    private MovimientoCajaEntidad registrarMovimientoInterno(
            Long empresaId,
            Long cajaSesionId,
            Long citaId,
            String tipoMovimiento,
            String metodoPago,
            BigDecimal monto,
            String concepto,
            String referencia,
            String observaciones,
            Long usuarioId
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
        movimiento.setRegistradoEn(LocalDateTime.now());
        return movimientoCajaRepositorio.save(movimiento);
    }

    private BigDecimal calcularSaldoEsperado(CajaSesionEntidad sesion) {
        List<MovimientoCajaEntidad> movimientos = movimientoCajaRepositorio.findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(sesion.getEmpresaId(), sesion.getId());
        BigDecimal saldo = montoSeguro(sesion.getMontoInicial());
        for (MovimientoCajaEntidad movimiento : movimientos) {
            switch (movimiento.getTipoMovimiento()) {
                case "INGRESO_CITA", "INGRESO_EXTRA", "AJUSTE_POSITIVO" -> saldo = saldo.add(movimiento.getMonto());
                case "GASTO_MENOR", "RETIRO", "AJUSTE_NEGATIVO" -> saldo = saldo.subtract(movimiento.getMonto());
                default -> {
                }
            }
        }
        return saldo;
    }

    private CitaPorCobrarResponse mapearCitaPorCobrar(CitaEntidad cita) {
        ClienteEntidad cliente = clienteRepositorio.findById(cita.getClienteId()).orElse(null);
        BigDecimal pagado = montoSeguro(pagoCitaRepositorio.sumarPagadoPorCita(cita.getEmpresaId(), cita.getId()));
        BigDecimal pendiente = cita.getPrecio().subtract(pagado);
        return new CitaPorCobrarResponse(
                cita.getId(),
                cliente != null ? cliente.getNombreCompleto() : "Cliente",
                cliente != null ? cliente.getTelefono() : null,
                servicioRepositorio.findById(cita.getServicioId()).map(servicio -> servicio.getNombre()).orElse("Servicio"),
                sucursalRepositorio.findById(cita.getSucursalId()).map(sucursal -> sucursal.getNombre()).orElse("Sucursal"),
                cita.getInicio(),
                cita.getPrecio(),
                pagado,
                pendiente.max(BigDecimal.ZERO),
                cita.getMoneda(),
                cita.getEstado(),
                pendiente.compareTo(BigDecimal.ZERO) <= 0 ? "PAGADO" : (pagado.compareTo(BigDecimal.ZERO) > 0 ? "PARCIAL" : "PENDIENTE")
        );
    }

    private CajaSesionResponse mapearSesion(CajaSesionEntidad sesion) {
        return new CajaSesionResponse(
                sesion.getId(),
                sesion.getSucursalId(),
                sucursalRepositorio.findById(sesion.getSucursalId()).map(sucursal -> sucursal.getNombre()).orElse("Sucursal"),
                sesion.getEstado(),
                sesion.getMontoInicial(),
                sesion.getMontoEsperado(),
                sesion.getMontoContado(),
                sesion.getDiferencia(),
                sesion.getObservaciones(),
                sesion.getAbiertaPorUsuarioId(),
                sesion.getAbiertaEn(),
                sesion.getCerradaPorUsuarioId(),
                sesion.getCerradaEn()
        );
    }

    private CajaSesionResponse mapearSesionConEsperado(CajaSesionEntidad sesion) {
        sesion.setMontoEsperado(calcularSaldoEsperado(sesion));
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
                pago.getRegistradoPorUsuarioId(),
                pago.getRegistradoEn()
        );
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
                movimiento.getRegistradoPorUsuarioId(),
                movimiento.getRegistradoEn()
        );
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
