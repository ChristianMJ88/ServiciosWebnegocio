package com.techprotech.agenda.modulos.disponibilidad.aplicacion;
import java.time.LocalDate;
import java.util.List;
public record DiasDisponiblesResponse(LocalDate hoyLocal, LocalDate reservaHasta,
        List<LocalDate> dias, com.techprotech.agenda.modulos.disponibilidad.api.dto.ConfiguracionReservasResponse configuracion) {}
