package com.techprotech.agenda.modulos.plataforma.api;

import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioStripePlataforma;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publico/plataforma/stripe")
public class ControladorWebhookStripePlataforma {
    private final ServicioStripePlataforma stripe;
    public ControladorWebhookStripePlataforma(ServicioStripePlataforma stripe){this.stripe=stripe;}

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestHeader("Stripe-Signature") String firma,@RequestBody String payload){
        stripe.recibirWebhook(firma,payload);
        return ResponseEntity.ok().build();
    }
}
