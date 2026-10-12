package com.techprotech.agenda.modulos.admin.api;

import com.techprotech.agenda.modulos.admin.api.dto.*;
import com.techprotech.agenda.modulos.admin.aplicacion.ServicioBloqueoAcceso;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class ControladorBloqueoAcceso {
    private final ServicioBloqueoAcceso servicio;
    public ControladorBloqueoAcceso(ServicioBloqueoAcceso servicio) { this.servicio = servicio; }

    @PatchMapping("/usuarios-internos/{id}/bloqueo")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public BloqueoAccesoResponse interno(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable Long id,
                                        @Valid @RequestBody BloqueoAccesoRequest request) {
        return servicio.cambiar(actor, id, false, request);
    }

    @PatchMapping("/prestadores/{id}/bloqueo")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public BloqueoAccesoResponse prestador(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable Long id,
                                          @Valid @RequestBody BloqueoAccesoRequest request) {
        return servicio.cambiar(actor, id, true, request);
    }
}
