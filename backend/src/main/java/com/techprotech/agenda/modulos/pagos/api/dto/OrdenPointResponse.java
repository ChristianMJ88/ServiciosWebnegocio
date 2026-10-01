package com.techprotech.agenda.modulos.pagos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdenPointResponse(String id, Long ventaId, String folioVenta, Long terminalId, String terminalNombre,
                                 String estado, String detalleEstado, BigDecimal importe, String moneda,
                                 String proveedorOrdenId, String proveedorPagoId, LocalDateTime creadoEn,
                                 LocalDateTime actualizadoEn) {}
