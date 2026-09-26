package com.techprotech.agenda.modulos.pagos.api;

import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioWebhookMercadoPago;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publico/pagos/mercado-pago")
public class ControladorWebhookMercadoPago {
    private final ServicioWebhookMercadoPago servicio;

    public ControladorWebhookMercadoPago(ServicioWebhookMercadoPago servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(name = "x-signature", required = false) String firma,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "data_id", required = false) String dataIdAlterno,
            @RequestBody String payload
    ) {
        servicio.recibir(firma, requestId, dataId != null ? dataId : dataIdAlterno, payload);
        return ResponseEntity.ok().build();
    }
}
