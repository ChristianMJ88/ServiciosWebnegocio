package com.techprotech.agenda.modulos.sitio.api;

import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarResenasSitioRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.ResenasSitioAdminResponse;
import com.techprotech.agenda.modulos.sitio.aplicacion.ServicioResenasSitio;
import com.techprotech.agenda.modulos.sitio.seguridad.PermisosSitio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/configuracion-sitio/resenas")
@PreAuthorize(PermisosSitio.EXPRESION_PANEL_ADMIN)
public class ControladorResenasSitioAdmin {
    private final ServicioResenasSitio servicio;

    public ControladorResenasSitioAdmin(ServicioResenasSitio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public ResenasSitioAdminResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.obtenerParaAdministracion(usuario.empresaId());
    }

    @PatchMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public ResenasSitioAdminResponse actualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ActualizarResenasSitioRequest request
    ) {
        return servicio.actualizar(usuario.empresaId(), usuario.usuarioId(), request);
    }
}
