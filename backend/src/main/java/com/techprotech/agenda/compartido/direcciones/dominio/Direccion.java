package com.techprotech.agenda.compartido.direcciones.dominio;

public record Direccion(
        String calle,
        String numeroExterior,
        String numeroInterior,
        String colonia,
        String municipio,
        String estado,
        String codigoPostal,
        String codigoPais
) {}
