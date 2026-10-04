package com.techprotech.agenda.modulos.plataforma.api.dto;
public record EstadoStripePlataformaResponse(boolean habilitado, String ambiente, boolean credencialConfigurada,
        boolean webhookConfigurado, String webhookUrl) {}
