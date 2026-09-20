package com.techprotech.agenda.modulos.sitio.api;

import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarServiciosDestacadosRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.ServiciosDestacadosAdminResponse;
import com.techprotech.agenda.modulos.sitio.aplicacion.ServicioServiciosDestacadosSitio;
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

@RestController
@RequestMapping("/api/v1/admin/configuracion-sitio/servicios-destacados")
@PreAuthorize(PermisosSitio.EXPRESION_PANEL_ADMIN)
public class ControladorServiciosDestacadosSitioAdmin {
    private final ServicioServiciosDestacadosSitio servicio;

    public ControladorServiciosDestacadosSitioAdmin(ServicioServiciosDestacadosSitio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public ServiciosDestacadosAdminResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.obtenerParaAdministracion(usuario.empresaId());
    }

    @PatchMapping
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public ServiciosDestacadosAdminResponse actualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ActualizarServiciosDestacadosRequest request
    ) {
        return servicio.actualizar(usuario.empresaId(), usuario.usuarioId(), request);
    }
}
