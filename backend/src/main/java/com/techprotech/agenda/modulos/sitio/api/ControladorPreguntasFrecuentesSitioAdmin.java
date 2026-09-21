package com.techprotech.agenda.modulos.sitio.api;

import com.techprotech.agenda.modulos.sitio.api.dto.*;
import com.techprotech.agenda.modulos.sitio.aplicacion.ServicioPreguntasFrecuentesSitio;
import com.techprotech.agenda.modulos.sitio.seguridad.PermisosSitio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/configuracion-sitio/preguntas-frecuentes")
@PreAuthorize(PermisosSitio.EXPRESION_PANEL_ADMIN)
public class ControladorPreguntasFrecuentesSitioAdmin {
    private final ServicioPreguntasFrecuentesSitio servicio;
    public ControladorPreguntasFrecuentesSitioAdmin(ServicioPreguntasFrecuentesSitio servicio) { this.servicio = servicio; }

    @GetMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public List<PreguntaFrecuenteSitioAdminResponse> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.obtenerParaAdministracion(usuario.empresaId());
    }

    @PatchMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public List<PreguntaFrecuenteSitioAdminResponse> actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ActualizarPreguntasFrecuentesSitioRequest request) {
        return servicio.actualizar(usuario.empresaId(), usuario.usuarioId(), request);
    }
}
