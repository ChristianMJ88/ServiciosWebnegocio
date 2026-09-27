package com.techprotech.agenda.compartido.direcciones.api;

import com.techprotech.agenda.compartido.direcciones.api.dto.ValidarDireccionRequest;
import com.techprotech.agenda.compartido.direcciones.aplicacion.ServicioValidacionDirecciones;
import com.techprotech.agenda.compartido.direcciones.dominio.ResultadoValidacionDireccion;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publico/direcciones")
public class ControladorDireccionesPublicas {
    private final ServicioValidacionDirecciones servicio;

    public ControladorDireccionesPublicas(ServicioValidacionDirecciones servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/configuracion")
    public ServicioValidacionDirecciones.Configuracion configuracion() {
        return servicio.configuracion();
    }

    @PostMapping("/validar")
    public ResultadoValidacionDireccion validar(@Valid @RequestBody ValidarDireccionRequest request) {
        return servicio.validar(request.direccion());
    }
}
