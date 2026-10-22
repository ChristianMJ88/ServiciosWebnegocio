package com.techprotech.agenda.modulos.dominios.api.dto;
import jakarta.validation.constraints.*;
public record RegistroDns(@NotBlank @Pattern(regexp="A|AAAA|CNAME|TXT") String tipo,
 @NotBlank @Size(max=253) String nombre,@NotBlank @Size(max=512) String valor) {}
