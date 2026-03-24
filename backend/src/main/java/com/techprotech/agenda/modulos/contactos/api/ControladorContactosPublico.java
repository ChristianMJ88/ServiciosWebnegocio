package com.techprotech.agenda.modulos.contactos.api;

import com.techprotech.agenda.modulos.contactos.api.dto.CrearSolicitudContactoRequest;
import com.techprotech.agenda.modulos.contactos.api.dto.SolicitudContactoCreadaResponse;
import com.techprotech.agenda.modulos.contactos.aplicacion.ServicioContactos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publico/contactos")
public class ControladorContactosPublico {

    private final ServicioContactos servicioContactos;

    public ControladorContactosPublico(ServicioContactos servicioContactos) {
        this.servicioContactos = servicioContactos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudContactoCreadaResponse crear(@Valid @RequestBody CrearSolicitudContactoRequest request) {
        return servicioContactos.crearSolicitud(request);
    }
}
