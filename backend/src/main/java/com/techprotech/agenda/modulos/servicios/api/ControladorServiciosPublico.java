package com.techprotech.agenda.modulos.servicios.api;

import com.techprotech.agenda.modulos.servicios.aplicacion.ServicioCatalogoServicios;
import com.techprotech.agenda.modulos.servicios.aplicacion.CatalogoServiciosPublicoResponse;
import com.techprotech.agenda.modulos.servicios.aplicacion.PrestadorPublicoResponse;
import com.techprotech.agenda.modulos.servicios.aplicacion.ServicioPublicoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/publico/servicios")
public class ControladorServiciosPublico {

    private final com.techprotech.agenda.modulos.sitio.aplicacion.ResolutorTenantPublico tenant;

    private final ServicioCatalogoServicios servicioCatalogoServicios;

    public ControladorServiciosPublico(ServicioCatalogoServicios servicioCatalogoServicios,
            com.techprotech.agenda.modulos.sitio.aplicacion.ResolutorTenantPublico tenant) {
        this.tenant = tenant;
        this.servicioCatalogoServicios = servicioCatalogoServicios;
    }

    @GetMapping
    public List<ServicioPublicoResponse> listar(@org.springframework.web.bind.annotation.RequestHeader("X-Tenant-Slug") String slug, @RequestParam Long sucursalId) {
        return servicioCatalogoServicios.listarPorSucursal(tenant.resolver(slug,null),sucursalId);
    }

    @GetMapping("/sitio/{slug}")
    public CatalogoServiciosPublicoResponse obtenerCatalogoPorSlug(@PathVariable String slug) {
        return servicioCatalogoServicios.obtenerCatalogoPorSlug(slug);
    }

    @GetMapping("/prestadores")
    public List<PrestadorPublicoResponse> listarPrestadores(
            @org.springframework.web.bind.annotation.RequestHeader("X-Tenant-Slug") String slug,
            @RequestParam Long empresaId,
            @RequestParam Long sucursalId,
            @RequestParam(required = false) Long servicioId
    ) {
        return servicioCatalogoServicios.listarPrestadores(tenant.resolver(slug,empresaId), sucursalId, servicioId);
    }
}
