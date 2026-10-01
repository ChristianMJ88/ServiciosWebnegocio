package com.techprotech.agenda.modulos.pagos.dominio;

public interface ProveedorPagosPort {
    ProveedorPago proveedor();

    ResultadoCobro crearCobro(SolicitudCobro solicitud);

    java.util.List<MetodoPagoProveedor> listarMetodosPago(Long empresaId);

    ResultadoCobro consultarCobro(Long empresaId, String referenciaExterna);

    ResultadoReembolso reembolsar(SolicitudReembolso solicitud);

    EventoPagoProveedor validarWebhook(WebhookProveedorPago webhook);
}
