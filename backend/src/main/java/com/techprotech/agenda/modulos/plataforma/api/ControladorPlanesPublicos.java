package com.techprotech.agenda.modulos.plataforma.api;

import com.techprotech.agenda.modulos.plataforma.api.dto.PlanPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioSuscripcionesPlataforma;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/publico/plataforma/planes")
public class ControladorPlanesPublicos {
    private final ServicioSuscripcionesPlataforma servicio;
    public ControladorPlanesPublicos(ServicioSuscripcionesPlataforma servicio) { this.servicio=servicio; }
    @GetMapping
    public List<PlanPlataformaResponse> listar() { return servicio.planesPublicos(); }
}
