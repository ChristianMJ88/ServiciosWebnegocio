package com.techprotech.agenda.compartido.direcciones.api.dto;

import com.techprotech.agenda.compartido.direcciones.dominio.Direccion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ValidarDireccionRequest(
        @NotBlank @Size(max = 180) String calle,
        @NotBlank @Size(max = 30) String numeroExterior,
        @Size(max = 30) String numeroInterior,
        @NotBlank @Size(max = 150) String colonia,
        @NotBlank @Size(max = 150) String municipio,
        @NotBlank @Size(max = 120) String estado,
        @NotBlank @Size(max = 12) String codigoPostal,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String codigoPais
) {
    public Direccion direccion() {
        return new Direccion(calle, numeroExterior, numeroInterior, colonia, municipio, estado, codigoPostal, codigoPais);
    }
}
