package com.techprotech.agenda.modulos.plataforma.api;

import com.techprotech.agenda.modulos.plataforma.api.dto.ResumenPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.TenantPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioConsultaPlataforma;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plataforma")
public class ControladorPlataforma {
    private final ServicioConsultaPlataforma servicio;

    public ControladorPlataforma(ServicioConsultaPlataforma servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('PLATAFORMA_FINANZAS_VER')")
    public ResumenPlataformaResponse resumen() { return servicio.resumen(); }

    @GetMapping("/tenants")
    @PreAuthorize("hasAuthority('PLATAFORMA_TENANTS_VER')")
    public List<TenantPlataformaResponse> tenants() { return servicio.tenants(); }
}
