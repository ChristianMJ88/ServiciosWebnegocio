package com.techprotech.agenda.modulos.citas.api;

import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CitasMultiplesCreadasResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitasMultiplesRequest;
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
    private final com.techprotech.agenda.modulos.citas.aplicacion.ServicioCotizacionCitas cotizador;
    private final com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa capacidades;

    public ControladorCitasPublico(ServicioCitas servicioCitas,
            com.techprotech.agenda.modulos.citas.aplicacion.ServicioCotizacionCitas cotizador,
            com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa capacidades) {
        this.servicioCitas = servicioCitas;
        this.cotizador = cotizador;
        this.capacidades = capacidades;
    }

    @PostMapping("/cotizar")
    public com.techprotech.agenda.modulos.citas.api.dto.ResultadoCotizacionCitas cotizar(
            @Valid @RequestBody com.techprotech.agenda.modulos.citas.api.dto.CotizarCitasRequest request) {
        capacidades.exigirSuscripcion(request.empresaId());
        return cotizador.cotizar(request.empresaId(),request.sucursalId(),request.promocionId(),request.items());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaCreadaResponse crear(@Valid @RequestBody CrearCitaRequest request) {
        return servicioCitas.crearCita(request);
    }

    @PostMapping("/multiples")
    @ResponseStatus(HttpStatus.CREATED)
    public CitasMultiplesCreadasResponse crearMultiples(@Valid @RequestBody CrearCitasMultiplesRequest request) {
        return servicioCitas.crearCitasMultiples(request);
    }
}
