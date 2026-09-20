package com.techprotech.agenda.modulos.sitio.api;

import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarBloquesSitioRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.BloqueSitioAdminResponse;
import com.techprotech.agenda.modulos.sitio.aplicacion.ServicioConstructorSitio;
import com.techprotech.agenda.modulos.sitio.seguridad.PermisosSitio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/configuracion-sitio/bloques")
@PreAuthorize(PermisosSitio.EXPRESION_PANEL_ADMIN)
public class ControladorConstructorSitioAdmin {
    private final ServicioConstructorSitio servicioConstructorSitio;

    public ControladorConstructorSitioAdmin(ServicioConstructorSitio servicioConstructorSitio) {
        this.servicioConstructorSitio = servicioConstructorSitio;
    }

    @GetMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public List<BloqueSitioAdminResponse> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioConstructorSitio.obtenerParaAdministracion(usuario.empresaId());
    }

    @PatchMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public List<BloqueSitioAdminResponse> actualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ActualizarBloquesSitioRequest request
    ) {
        return servicioConstructorSitio.actualizar(usuario.empresaId(), usuario.usuarioId(), request);
    }
}
