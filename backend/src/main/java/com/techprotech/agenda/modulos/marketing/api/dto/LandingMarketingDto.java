package com.techprotech.agenda.modulos.marketing.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record LandingMarketingDto(
 @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=80) String slug,
 boolean publicado, Long version,
 @NotBlank @Size(max=100) String etiqueta,
 @NotBlank @Size(max=180) String titulo,
 @NotBlank @Size(max=500) String descripcion,
 @NotBlank @Size(max=60) String ctaPrincipal,
 @NotBlank @Size(max=60) String ctaSecundario,
 @NotBlank @Size(max=180) String tituloFinal,
 @Size(max=1000) String imagenUrl, @Size(max=180) String imagenAlt,
 @NotBlank @Size(max=180) String seoTitulo, @NotBlank @Size(max=320) String seoDescripcion,
 @NotNull @Size(min=1,max=12) List<@NotNull @Valid Bloque> beneficios,
 @NotNull @Size(min=1,max=12) List<@NotNull @Valid Bloque> funcionalidades,
 @NotNull @Size(min=1,max=6) List<@NotNull @Valid Bloque> pasos,
 @NotNull @Size(max=12) List<@NotNull @Valid Pregunta> preguntas
) {
 public record Bloque(@NotBlank @Size(max=100) String titulo,@NotBlank @Size(max=500) String descripcion) {}
 public record Pregunta(@NotBlank @Size(max=180) String pregunta,@NotBlank @Size(max=1000) String respuesta) {}
}
