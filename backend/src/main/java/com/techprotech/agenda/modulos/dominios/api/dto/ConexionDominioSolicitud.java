package com.techprotech.agenda.modulos.dominios.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record ConexionDominioSolicitud(@NotNull @PositiveOrZero Long version,
 @NotEmpty @Size(max=20) List<@Valid RegistroDns> registros) {}
