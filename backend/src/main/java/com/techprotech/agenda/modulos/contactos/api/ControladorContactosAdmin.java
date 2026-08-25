package com.techprotech.agenda.modulos.contactos.api;

import com.techprotech.agenda.modulos.contactos.api.dto.ActualizarEstadoSolicitudContactoRequest;
import com.techprotech.agenda.modulos.contactos.api.dto.SolicitudContactoAdminResponse;
import com.techprotech.agenda.modulos.contactos.api.dto.MetadatosContactosResponse;
import com.techprotech.agenda.modulos.contactos.aplicacion.EstadoSolicitudContacto;
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
@PreAuthorize("hasAuthority('PANEL_ADMIN_ACCESO')")
public class ControladorContactosAdmin {

    private final ServicioGestionSolicitudesContacto servicioGestionSolicitudesContacto;

    public ControladorContactosAdmin(ServicioGestionSolicitudesContacto servicioGestionSolicitudesContacto) {
        this.servicioGestionSolicitudesContacto = servicioGestionSolicitudesContacto;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTACTOS_ADMIN_VER')")
    public List<SolicitudContactoAdminResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionSolicitudesContacto.listarPorEmpresa(usuario.empresaId());
    }

    @GetMapping("/metadatos")
    @PreAuthorize("hasAuthority('CONTACTOS_ADMIN_VER')")
    public MetadatosContactosResponse metadatos() {
        return new MetadatosContactosResponse(
                java.util.Arrays.stream(EstadoSolicitudContacto.values())
                        .map(estado -> new MetadatosContactosResponse.EstadoContactoResponse(estado.name(), estado.etiqueta()))
                        .toList(),
                EstadoSolicitudContacto.NUEVO.name(),
                EstadoSolicitudContacto.EN_PROCESO.name(),
                EstadoSolicitudContacto.ATENDIDO.name()
        );
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('CONTACTOS_ADMIN_VER')")
    public SolicitudContactoAdminResponse actualizarEstado(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoSolicitudContactoRequest request
    ) {
        return servicioGestionSolicitudesContacto.actualizarEstado(usuario.empresaId(), id, request.estado());
    }
}
