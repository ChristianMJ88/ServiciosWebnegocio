package com.techprotech.agenda.modulos.citas.dominio.eventos;

import java.time.Instant;
import java.time.LocalDateTime;

/** Snapshot inmutable: momento es UTC; inicio/fin conservan la hora de agenda del negocio. */
public record DatosEventoCita(Long empresaId, Long sucursalId, Long citaId, Long clienteId,
        OrigenEventoCita origen, Long actorId, Instant momento, LocalDateTime inicio, LocalDateTime fin) {}
