package com.techprotech.agenda.modulos.citas.dominio.eventos;

public record CitaConfirmada(DatosEventoCita datos, boolean avisarWhatsapp, boolean avisarNegocio) implements EventoCita {}
