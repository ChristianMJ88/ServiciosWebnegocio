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

    private final com.techprotech.agenda.modulos.sitio.aplicacion.ResolutorTenantPublico tenant;

    private final ServicioCitas servicioCitas;
    private final com.techprotech.agenda.modulos.citas.aplicacion.ServicioCotizacionReservaMixta reservaMixta;
    private final com.techprotech.agenda.modulos.citas.aplicacion.ServicioCotizacionCitas cotizador;
    private final com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa capacidades;

    public ControladorCitasPublico(ServicioCitas servicioCitas,
            com.techprotech.agenda.modulos.citas.aplicacion.ServicioCotizacionCitas cotizador,
            com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa capacidades,
            com.techprotech.agenda.modulos.sitio.aplicacion.ResolutorTenantPublico tenant, com.techprotech.agenda.modulos.citas.aplicacion.ServicioCotizacionReservaMixta reservaMixta) {
        this.tenant = tenant;this.reservaMixta=reservaMixta;
        this.servicioCitas = servicioCitas;
        this.cotizador = cotizador;
        this.capacidades = capacidades;
    }

    @PostMapping("/cotizar")
    public com.techprotech.agenda.modulos.citas.api.dto.ResultadoCotizacionCitas cotizar(
            @org.springframework.web.bind.annotation.RequestHeader("X-Tenant-Slug") String slug,
            @Valid @RequestBody com.techprotech.agenda.modulos.citas.api.dto.CotizarCitasRequest request) {
        Long empresaId = tenant.resolver(slug, request.empresaId());
        for(var item:request.items()) tenant.validarServicio(empresaId,request.sucursalId(),item.servicioId(),null);
        capacidades.exigirSuscripcion(empresaId);
        return reservaMixta.cotizar(new com.techprotech.agenda.modulos.citas.api.dto.CotizarCitasRequest(empresaId, request.sucursalId(), request.promocionId(), request.items(), request.productos()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaCreadaResponse crear(@org.springframework.web.bind.annotation.RequestHeader("X-Tenant-Slug") String slug, @Valid @RequestBody CrearCitaRequest request) {
        Long empresaId = tenant.resolver(slug,request.empresaId());
        tenant.validarServicio(empresaId,request.sucursalId(),request.servicioId(),request.prestadorId());
        return servicioCitas.crearCitaPublica(new CrearCitaRequest(empresaId, request.sucursalId(), request.servicioId(), request.prestadorId(), request.nombreCliente(), request.correoCliente(), request.telefonoCliente(), request.inicio(), request.notas(), request.promocionId()));
    }

    @PostMapping("/multiples")
    @ResponseStatus(HttpStatus.CREATED)
    public CitasMultiplesCreadasResponse crearMultiples(@org.springframework.web.bind.annotation.RequestHeader("X-Tenant-Slug") String slug, @Valid @RequestBody CrearCitasMultiplesRequest request) {
        Long empresaId = tenant.resolver(slug,request.empresaId());
        for(var item:request.items()) tenant.validarServicio(empresaId,request.sucursalId(),item.servicioId(),item.prestadorId());
        return servicioCitas.crearCitasMultiplesPublicas(new CrearCitasMultiplesRequest(empresaId, request.sucursalId(), request.nombreCliente(), request.correoCliente(), request.telefonoCliente(), request.notas(), request.items(), request.promocionId()));
    }
}
