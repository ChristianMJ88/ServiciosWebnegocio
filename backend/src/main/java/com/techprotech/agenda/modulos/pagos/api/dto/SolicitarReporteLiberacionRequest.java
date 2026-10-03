package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record SolicitarReporteLiberacionRequest(@NotNull OffsetDateTime desde, @NotNull OffsetDateTime hasta) {}
