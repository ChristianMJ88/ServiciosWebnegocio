package com.techprotech.agenda.modulos.sitio.api;

import com.techprotech.agenda.modulos.sitio.aplicacion.ServicioSitioPublico;
import com.techprotech.agenda.modulos.sitio.aplicacion.SitioPublicoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publico/sitio")
public class ControladorSitioPublico {

    private final ServicioSitioPublico servicioSitioPublico;

    public ControladorSitioPublico(ServicioSitioPublico servicioSitioPublico) {
        this.servicioSitioPublico = servicioSitioPublico;
    }

    @GetMapping("/{slug}")
    public SitioPublicoResponse obtenerPorSlug(@PathVariable String slug) {
        return servicioSitioPublico.obtenerPorSlug(slug);
    }

    @GetMapping("/host/{hostname}")
    public SitioPublicoResponse obtenerPorHostname(@PathVariable String hostname) {
        return servicioSitioPublico.obtenerPorHostname(hostname);
    }
}
