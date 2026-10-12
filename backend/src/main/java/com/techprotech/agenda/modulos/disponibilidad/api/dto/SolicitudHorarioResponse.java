package com.techprotech.agenda.modulos.disponibilidad.api.dto;
import java.time.LocalDateTime;
public record SolicitudHorarioResponse(Long id,Long prestadorId,String prestadorNombre,Long reglaId,HorarioSemanalRequest propuesta,String estado,LocalDateTime creadaEn) {}
