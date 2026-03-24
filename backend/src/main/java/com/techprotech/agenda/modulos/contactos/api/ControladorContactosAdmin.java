package com.techprotech.agenda.modulos.contactos.api;

import com.techprotech.agenda.modulos.contactos.api.dto.ActualizarEstadoSolicitudContactoRequest;
import com.techprotech.agenda.modulos.contactos.api.dto.SolicitudContactoAdminResponse;
import com.techprotech.agenda.modulos.contactos.aplicacion.ServicioGestionSolicitudesContacto;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/contactos")
@PreAuthorize("hasRole('ADMIN')")
public class ControladorContactosAdmin {

    private final ServicioGestionSolicitudesContacto servicioGestionSolicitudesContacto;

    public ControladorContactosAdmin(ServicioGestionSolicitudesContacto servicioGestionSolicitudesContacto) {
        this.servicioGestionSolicitudesContacto = servicioGestionSolicitudesContacto;
    }

    @GetMapping
    public List<SolicitudContactoAdminResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionSolicitudesContacto.listarPorEmpresa(usuario.empresaId());
    }

    @PatchMapping("/{id}/estado")
    public SolicitudContactoAdminResponse actualizarEstado(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoSolicitudContactoRequest request
    ) {
        return servicioGestionSolicitudesContacto.actualizarEstado(usuario.empresaId(), id, request.estado());
    }
}
