package com.techprotech.agenda.modulos.sitio.api.dto;

public record PreguntaFrecuenteSitioAdminResponse(Long id, String pregunta, String respuesta, boolean activa, int orden) {}
