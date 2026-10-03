package com.techprotech.agenda.modulos.pagos.api.dto;

import java.util.List;

public record ConfiguracionReporteLiberacionResponse(String prefijo, String zonaHoraria, boolean programado,
        String idioma, List<String> columnas, List<String> columnasFaltantes, boolean listaParaConciliar,
        String frecuenciaTipo, Integer frecuenciaHora, String frecuenciaValor) {}
