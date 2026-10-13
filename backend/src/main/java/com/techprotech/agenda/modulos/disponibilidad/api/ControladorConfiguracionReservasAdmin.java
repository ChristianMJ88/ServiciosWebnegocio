package com.techprotech.agenda.modulos.disponibilidad.api;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConfiguracionReservas;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ConfiguracionReservasResponse;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/disponibilidad/configuracion-reservas")
@PreAuthorize("hasAuthority('PANEL_ADMIN_ACCESO') and hasAuthority('PRESTADORES_GESTIONAR')")
public class ControladorConfiguracionReservasAdmin {
    private final ServicioConfiguracionReservas configuracion;
    public ControladorConfiguracionReservasAdmin(ServicioConfiguracionReservas configuracion) { this.configuracion=configuracion; }
    @GetMapping public ConfiguracionReservasResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) { return configuracion.obtener(usuario.empresaId()); }
    @PatchMapping public ConfiguracionReservasResponse guardar(@AuthenticationPrincipal UsuarioAutenticado usuario,@Valid @RequestBody ConfiguracionReservasResponse request) { return configuracion.guardar(usuario.empresaId(),request); }
}
