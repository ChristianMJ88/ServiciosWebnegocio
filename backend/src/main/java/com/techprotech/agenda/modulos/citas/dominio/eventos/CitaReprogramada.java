package com.techprotech.agenda.modulos.citas.dominio.eventos;

import java.time.LocalDateTime;

public record CitaReprogramada(DatosEventoCita datos, LocalDateTime inicioAnterior,
        LocalDateTime finAnterior) implements EventoCita {}
