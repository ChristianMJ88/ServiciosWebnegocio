package com.techprotech.agenda.modulos.autenticacion.api;

import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RegistrarClienteRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioAutenticacion;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAutenticacion;

    public ControladorAutenticacion(ServicioAutenticacion servicioAutenticacion) {
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @PostMapping("/iniciar-sesion")
    public ResponseEntity<RespuestaTokenJwt> iniciarSesion(@Valid @RequestBody IniciarSesionRequest request) {
        return ResponseEntity.ok(servicioAutenticacion.iniciarSesion(request));
    }

    @PostMapping("/registrar-cliente")
    public ResponseEntity<Void> registrarCliente(@Valid @RequestBody RegistrarClienteRequest request) {
        servicioAutenticacion.registrarCliente(request);
        return ResponseEntity.noContent().build();
    }
}

