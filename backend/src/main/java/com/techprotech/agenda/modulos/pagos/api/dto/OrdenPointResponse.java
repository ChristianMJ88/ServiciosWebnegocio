package com.techprotech.agenda.modulos.pagos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdenPointResponse(String id, Long ventaId, String folioVenta, Long terminalId, String terminalNombre,
                                 String estado, String detalleEstado, BigDecimal importe, String moneda,
                                 String proveedorOrdenId, String proveedorPagoId, LocalDateTime creadoEn,
                                 LocalDateTime actualizadoEn, boolean simulacionDisponible, BigDecimal efectivoPrevisto,boolean puedeReintentar,boolean puedeResolver,boolean requiereRevision,boolean planInvalido,boolean puedeReembolsar) {
    public OrdenPointResponse conAcciones(com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado usuario) {
        boolean incierta=proveedorOrdenId==null && java.util.Set.of("CREANDO","REQUIERE_REVISION").contains(estado);
        return new OrdenPointResponse(id,ventaId,folioVenta,terminalId,terminalNombre,estado,detalleEstado,importe,moneda,proveedorOrdenId,proveedorPagoId,creadoEn,actualizadoEn,simulacionDisponible,efectivoPrevisto,
            incierta && "CREANDO".equals(estado) && usuario.tienePermiso("CAJA_COBRAR"),
            incierta && "REQUIERE_REVISION".equals(estado) && usuario.tienePermiso(com.techprotech.agenda.modulos.pagos.seguridad.PermisosPagos.CONFIGURAR),requiereRevision,planInvalido,puedeReembolsar);
    }
}
