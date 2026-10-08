package com.techprotech.agenda.compartido.correo;

import java.math.BigDecimal;

public record SuscripcionCorreoPayload(String destinatario, String negocio, String plan, String titulo,
        String descripcion, String fecha, BigDecimal importe, String moneda, String referencia) {}
