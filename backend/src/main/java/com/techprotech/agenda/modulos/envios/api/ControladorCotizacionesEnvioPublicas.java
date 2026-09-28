package com.techprotech.agenda.modulos.envios.api;

import com.techprotech.agenda.modulos.envios.api.dto.CotizacionEnvioPublicaResponse;
import com.techprotech.agenda.modulos.envios.api.dto.CotizarEnvioPublicoRequest;
import com.techprotech.agenda.modulos.envios.aplicacion.ServicioCotizacionesEnvio;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publico/empresas/{slug}/envios/cotizaciones")
public class ControladorCotizacionesEnvioPublicas {
    private final ServicioCotizacionesEnvio servicio;

    public ControladorCotizacionesEnvioPublicas(ServicioCotizacionesEnvio servicio) { this.servicio = servicio; }

    @PostMapping
    public CotizacionEnvioPublicaResponse cotizar(@PathVariable String slug,
                                                  @Valid @RequestBody CotizarEnvioPublicoRequest request) {
        return servicio.cotizar(slug, request);
    }
}
