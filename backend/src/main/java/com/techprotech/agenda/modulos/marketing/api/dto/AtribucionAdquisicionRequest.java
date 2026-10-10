package com.techprotech.agenda.modulos.marketing.api.dto;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record AtribucionAdquisicionRequest(
 @Size(max=80) @Pattern(regexp="[A-Za-z][A-Za-z0-9_-]{0,79}") String source,
 @Size(max=80) @Pattern(regexp="[A-Za-z][A-Za-z0-9_-]{0,79}") String medium,
 @Size(max=80) @Pattern(regexp="[A-Za-z][A-Za-z0-9_-]{0,79}") String campaign,
 @Size(max=80) @Pattern(regexp="[A-Za-z][A-Za-z0-9_-]{0,79}") String content,
 @Size(max=80) @Pattern(regexp="[A-Za-z][A-Za-z0-9_-]{0,79}") String term,
 @Size(max=80) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String landingVertical
) {}
