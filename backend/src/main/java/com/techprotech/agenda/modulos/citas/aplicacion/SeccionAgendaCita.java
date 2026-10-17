package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.caja.aplicacion.SaldoCita;

public enum SeccionAgendaCita {
    ACTIVA, FINALIZADA, CANCELADA;
    public static SeccionAgendaCita de(CitaEntidad cita, SaldoCita saldo) {
        if ("CANCELADA".equals(cita.getEstado())) return CANCELADA;
        if ("FINALIZADA".equals(cita.getEstado()) || "NO_ASISTIO".equals(cita.getEstado())) return FINALIZADA;
        if (cita.getCheckInEn() != null && saldo != null && saldo.pendiente().signum() == 0) return FINALIZADA;
        return ACTIVA;
    }
}
