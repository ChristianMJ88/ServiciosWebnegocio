package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GuardarTerminalPointRequest(@NotBlank @Size(max = 180) String terminalId,
                                          @NotNull Long sucursalId,
                                          @NotBlank @Size(max = 160) String nombre,
                                          boolean activa) {}
