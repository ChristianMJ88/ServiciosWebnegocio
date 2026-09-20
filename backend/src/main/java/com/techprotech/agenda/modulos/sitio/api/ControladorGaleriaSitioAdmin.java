package com.techprotech.agenda.modulos.sitio.api;

import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarGaleriaSitioRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.GaleriaSitioAdminResponse;
import com.techprotech.agenda.modulos.sitio.aplicacion.ServicioGaleriaSitio;
import com.techprotech.agenda.modulos.sitio.seguridad.PermisosSitio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/configuracion-sitio/galeria")
@PreAuthorize(PermisosSitio.EXPRESION_PANEL_ADMIN)
public class ControladorGaleriaSitioAdmin {
    private final ServicioGaleriaSitio servicio;

    public ControladorGaleriaSitioAdmin(ServicioGaleriaSitio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public GaleriaSitioAdminResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.obtenerParaAdministracion(usuario.empresaId());
    }

    @PatchMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public GaleriaSitioAdminResponse actualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ActualizarGaleriaSitioRequest request
    ) {
        return servicio.actualizar(usuario.empresaId(), usuario.usuarioId(), request);
    }
}
