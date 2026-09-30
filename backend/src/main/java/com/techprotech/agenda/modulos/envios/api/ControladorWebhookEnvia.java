package com.techprotech.agenda.modulos.envios.api;

import com.techprotech.agenda.modulos.envios.aplicacion.ServicioWebhookEnvia;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publico/envios/envia/webhooks")
public class ControladorWebhookEnvia {
    private final ServicioWebhookEnvia servicio;

    public ControladorWebhookEnvia(ServicioWebhookEnvia servicio) { this.servicio = servicio; }

    @PostMapping("/{token}")
    public ResponseEntity<Void> recibir(
            @PathVariable String token,
            @RequestHeader(name = "X-Webhook-Id", required = false) String entregaId,
            @RequestHeader(name = "X-Webhook-Event", required = false) String evento,
            @RequestHeader(name = "X-Webhook-Timestamp", required = false) String timestamp,
            @RequestHeader(name = "X-Webhook-Signature", required = false) String firma,
            @RequestBody String payload) {
        servicio.recibir(token, entregaId, evento, timestamp, firma, payload);
        return ResponseEntity.accepted().build();
    }
}
