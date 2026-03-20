package com.techprotech.agenda.modulos.citas.api;

import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publico/citas")
public class ControladorCitasPublico {

    private final ServicioCitas servicioCitas;

    public ControladorCitasPublico(ServicioCitas servicioCitas) {
        this.servicioCitas = servicioCitas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaCreadaResponse crear(@Valid @RequestBody CrearCitaRequest request) {
        return servicioCitas.crearCita(request);
    }
}

