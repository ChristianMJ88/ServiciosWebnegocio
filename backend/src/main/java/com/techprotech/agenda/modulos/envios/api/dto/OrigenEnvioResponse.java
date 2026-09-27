package com.techprotech.agenda.modulos.envios.api.dto;

public record OrigenEnvioResponse(
        Long sucursalId, String sucursalNombre, boolean sucursalActiva, boolean configurado, boolean activo,
        String nombreContacto, String telefono, String correo, String calle, String numeroExterior,
        String numeroInterior, String colonia, String municipio, String estado, String codigoPostal,
        String codigoPais, String referencias, String direccionFormateada, String direccionValidacionEstado
) {}
