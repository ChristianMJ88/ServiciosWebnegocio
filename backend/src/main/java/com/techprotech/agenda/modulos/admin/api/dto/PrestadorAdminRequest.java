package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.math.BigDecimal;

public record PrestadorAdminRequest(
        @NotNull Long sucursalId,
        @Email String correo,
        @Size(min = 8, max = 100) String contrasenaTemporal,
        @NotBlank @Size(max = 150) String nombreMostrar,
        @Size(max = 500) String biografia,
        @Size(max = 500) String fotoUrl,
        @Size(max = 20) String fotoEncuadre,
        @DecimalMin("1.00") @DecimalMax("2.00") BigDecimal fotoZoom,
        @Size(max = 20) String colorAgenda,
        boolean activo,
        List<Long> servicioIds,
        @jakarta.validation.constraints.Pattern(regexp="[a-zA-Z0-9._-]{3,80}") String nombreUsuario
) {
 public PrestadorAdminRequest(Long sucursalId,String correo,String contrasenaTemporal,String nombreMostrar,String biografia,String fotoUrl,String fotoEncuadre,BigDecimal fotoZoom,String colorAgenda,boolean activo,List<Long> servicioIds){this(sucursalId,correo,contrasenaTemporal,nombreMostrar,biografia,fotoUrl,fotoEncuadre,fotoZoom,colorAgenda,activo,servicioIds,null);}
}
