package com.techprotech.agenda.modulos.politicas.api;

import com.techprotech.agenda.modulos.politicas.api.dto.PoliticaPublicaResponse;
import com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/publico/empresas/{slug}/politicas")
@RequiredArgsConstructor
public class ControladorPoliticaPublica {
    private final ServicioPoliticaEmpresa servicio;
    @GetMapping public ResponseEntity<PoliticaPublicaResponse> obtener(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(servicio.publica(slug));
    }
}
