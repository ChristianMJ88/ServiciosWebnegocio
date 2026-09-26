package com.techprotech.agenda.modulos.pagos.api.dto;

import java.time.LocalDateTime;

public record ConexionProveedorPagoResponse(
        String proveedor,
        String estado,
        String ambiente,
        String cuentaExternaId,
        String correoCuenta,
        LocalDateTime conectadoEn,
        LocalDateTime tokenExpiraEn
) {}
