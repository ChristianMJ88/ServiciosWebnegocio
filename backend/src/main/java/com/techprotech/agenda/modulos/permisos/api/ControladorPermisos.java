package com.techprotech.agenda.modulos.permisos.api;
import com.techprotech.agenda.modulos.admin.api.dto.PermisoAdminResponse;
import com.techprotech.agenda.modulos.permisos.aplicacion.ServicioPermisos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/admin/permisos")
@PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
public class ControladorPermisos {
    private final ServicioPermisos servicio;
    public ControladorPermisos(ServicioPermisos servicio) { this.servicio = servicio; }
    @GetMapping public List<PermisoAdminResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.listarCatalogo(usuario.empresaId()).stream().map(p -> new PermisoAdminResponse(p.id(), p.codigo(), p.nombre(), p.descripcion())).toList();
    }
}
