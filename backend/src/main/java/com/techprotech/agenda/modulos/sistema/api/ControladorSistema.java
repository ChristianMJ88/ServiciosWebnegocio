package com.techprotech.agenda.modulos.sistema.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sistema")
public class ControladorSistema {

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return Map.of(
                "servicio", "agenda-backend",
                "estado", "OK",
                "fechaHora", OffsetDateTime.now()
        );
    }
}
