package com.techprotech.agenda.modulos.sitio.api;

import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarPromocionesSitioRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.PromocionSitioAdminResponse;
import com.techprotech.agenda.modulos.sitio.aplicacion.ServicioPromocionesSitio;
import com.techprotech.agenda.modulos.sitio.seguridad.PermisosSitio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/configuracion-sitio/promociones")
@PreAuthorize(PermisosSitio.EXPRESION_PANEL_ADMIN)
public class ControladorPromocionesSitioAdmin {
    private final ServicioPromocionesSitio servicio;

    public ControladorPromocionesSitioAdmin(ServicioPromocionesSitio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public List<PromocionSitioAdminResponse> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.obtenerParaAdministracion(usuario.empresaId());
    }

    @PatchMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public List<PromocionSitioAdminResponse> actualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ActualizarPromocionesSitioRequest request
    ) {
        return servicio.actualizar(usuario.empresaId(), usuario.usuarioId(), request);
    }
}
