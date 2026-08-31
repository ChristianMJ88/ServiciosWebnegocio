package com.techprotech.agenda.modulos.admin.api;

import com.techprotech.agenda.modulos.admin.api.dto.*;
import com.techprotech.agenda.modulos.admin.aplicacion.ServicioInvitacionesUsuario;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ControladorInvitacionesUsuario {
    private final ServicioInvitacionesUsuario servicio;
    public ControladorInvitacionesUsuario(ServicioInvitacionesUsuario servicio) { this.servicio = servicio; }

    @PostMapping("/admin/usuarios-internos/invitaciones")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public InvitacionUsuarioResponse invitar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @Valid @RequestBody UsuarioInternoAdminRequest request) {
        return servicio.invitar(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @GetMapping("/publico/invitaciones/usuario")
    public DetalleInvitacionUsuarioResponse consultar(@RequestParam String token) { return servicio.consultar(token); }

    @PostMapping("/publico/invitaciones/usuario/aceptar")
    public UsuarioInternoAdminResponse aceptar(@Valid @RequestBody AceptarInvitacionUsuarioRequest request) {
        return servicio.aceptar(request);
    }
}
