package com.techprotech.agenda.modulos.plataforma.api;

import com.techprotech.agenda.modulos.plataforma.api.dto.IniciarSesionPlataformaRequest;
import com.techprotech.agenda.modulos.plataforma.api.dto.SesionPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioAutenticacionPlataforma;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/plataforma/auth")
public class ControladorAutenticacionPlataforma {
    private final ServicioAutenticacionPlataforma servicio;

    public ControladorAutenticacionPlataforma(ServicioAutenticacionPlataforma servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/iniciar-sesion")
    public SesionPlataformaResponse iniciar(@Valid @RequestBody IniciarSesionPlataformaRequest request) {
        return servicio.iniciar(request);
    }
}
