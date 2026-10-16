package com.techprotech.agenda.modulos.disponibilidad.api.dto;
import java.time.OffsetDateTime;
public record SolicitudHorarioResponse(Long id,Long prestadorId,String prestadorNombre,Long reglaId,HorarioSemanalRequest propuesta,String estado,OffsetDateTime creadaEn) {}
