package com.techprotech.agenda.modulos.sucursales.aplicacion;
import jakarta.validation.constraints.*;
public record HorarioComercial(@NotNull @Min(1) @Max(7) Integer dia,
 @NotBlank @Pattern(regexp="^([01][0-9]|2[0-3]):[0-5][0-9]$") String apertura,
 @NotBlank @Pattern(regexp="^([01][0-9]|2[0-3]):[0-5][0-9]$") String cierre) {
 @com.fasterxml.jackson.annotation.JsonIgnore
 @AssertTrue(message="El cierre debe ser posterior a la apertura")
 public boolean isIntervaloValido(){return apertura==null||cierre==null||apertura.compareTo(cierre)<0;}
}
