package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import com.techprotech.agenda.modulos.caja.aplicacion.SaldoCita;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioSaldoCitas;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.util.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

/** Política de cancelación por el negocio. La cancelación del cliente conserva su política propia. */
@Service
public class ServicioCancelacionCita {
    private final CitaRepositorio citas;
    private final ServicioSaldoCitas saldos;
    private final PublicadorEventosCita eventos;
    private final Clock reloj;
    private final HistorialEstadoCitaRepositorio historial;

    public ServicioCancelacionCita(CitaRepositorio citas, ServicioSaldoCitas saldos,
            HistorialEstadoCitaRepositorio historial,
            PublicadorEventosCita eventos, Clock reloj) {
        this.citas = citas;
        this.saldos = saldos; this.historial = historial; this.eventos = eventos; this.reloj = reloj;
    }

    public boolean puedeCancelar(CitaEntidad cita, SaldoCita saldo) {
        return motivoRechazo(cita, saldo) == null;
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> elegibilidad(Long empresaId, Collection<CitaEntidad> consultadas) {
        var montos = saldos.calcular(empresaId, consultadas);
        var resultado = new HashMap<Long, Boolean>();
        for (var cita : consultadas) {
            if (empresaId.equals(cita.getEmpresaId()))
                resultado.put(cita.getId(), puedeCancelar(cita, montos.get(cita.getId())));
        }
        return Map.copyOf(resultado);
    }

    /** El bloqueo se conserva durante la transacción de la ruta que guarda estado, historial y outbox. */
    @Transactional
    public CitaEntidad bloquearYValidar(UsuarioAutenticado usuario, Long citaId) {
        if (usuario == null || usuario.empresaId() == null || usuario.esPlataforma())
            throw new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa");
        var cita = citas.bloquearPorIdYEmpresa(citaId, usuario.empresaId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa"));
        if (!usuario.puedeAccederSucursal(cita.getSucursalId()))
            throw new ResponseStatusException(NOT_FOUND, "La cita no existe en tu alcance");
        // Rechazar estados/check-in antes de consultar importes; ninguna ruta puede omitir la política.
        var saldo = estadosAdmitidos(cita) && cita.getCheckInEn() == null ? saldos.de(cita) : null;
        var motivo = motivoRechazo(cita, saldo);
        if (motivo != null) throw new ResponseStatusException(CONFLICT, motivo);
        return cita;
    }

    @Transactional
    public CitaEntidad cancelar(UsuarioAutenticado usuario, Long citaId, OrigenEventoCita origen) {
        if (origen != OrigenEventoCita.ADMIN && origen != OrigenEventoCita.RECEPCION)
            throw new ResponseStatusException(BAD_REQUEST, "Origen de cancelación del negocio no válido");
        var cita = bloquearYValidar(usuario, citaId);
        var anterior = cita.getEstado();
        cita.setEstado("CANCELADA");
        cita.setCanceladaEn(LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC));
        cita.setMotivoCancelacion(origen == OrigenEventoCita.ADMIN
                ? "Cancelada desde el panel administrativo" : "Cancelada desde recepción");
        citas.save(cita);
        var cambio = new HistorialEstadoCitaEntidad();
        cambio.setCitaId(citaId); cambio.setEstadoAnterior(anterior); cambio.setEstadoNuevo("CANCELADA");
        cambio.setCambiadoPorUsuarioId(usuario.usuarioId());
        cambio.setMotivo(origen == OrigenEventoCita.ADMIN ? "Cambio de estado realizado por administracion" : "Cancelación desde recepción");
        historial.save(cambio);
        eventos.publicar(new CitaCancelada(eventos.datos(cita, origen, usuario.usuarioId())));
        return cita;
    }

    private boolean estadosAdmitidos(CitaEntidad cita) {
        return cita != null && Set.of("PENDIENTE", "CONFIRMADA").contains(Objects.toString(cita.getEstado(), ""));
    }

    private String motivoRechazo(CitaEntidad cita, SaldoCita saldo) {
        if (!estadosAdmitidos(cita)) return "Solo puedes cancelar citas pendientes o confirmadas";
        if (cita.getCheckInEn() != null) return "La cita ya tiene check-in. Finaliza la atención o marca No asistió";
        if (saldo == null) return "No fue posible verificar los pagos de la cita";
        if (saldo.pagado().signum() > 0)
            return "La cita tiene pagos registrados; puedes reprogramarla. La cancelación con anticipo estará disponible próximamente";
        return null;
    }
}
