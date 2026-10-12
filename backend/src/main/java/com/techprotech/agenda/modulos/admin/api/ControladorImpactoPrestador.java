package com.techprotech.agenda.modulos.admin.api;

import com.techprotech.agenda.modulos.admin.api.dto.ImpactoPrestadorResponse;
import com.techprotech.agenda.modulos.admin.aplicacion.ServicioImpactoPrestador;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/prestadores")
public class ControladorImpactoPrestador {
    private final ServicioImpactoPrestador servicio;
    public ControladorImpactoPrestador(ServicioImpactoPrestador servicio) { this.servicio = servicio; }
    @GetMapping("/{id}/impacto")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public ImpactoPrestadorResponse consultar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable Long id) {
        return servicio.consultar(actor.empresaId(), id);
    }
}
