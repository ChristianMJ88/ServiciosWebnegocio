package com.techprotech.agenda.modulos.servicios.api;
import com.techprotech.agenda.modulos.servicios.aplicacion.ServicioPoliticasCobroServicios;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/servicios/politicas-cobro")
@PreAuthorize("hasAuthority('PANEL_ADMIN_ACCESO') and hasAuthority('SERVICIOS_GESTIONAR')")
public class ControladorPoliticasCobroAdmin {
    private final ServicioPoliticasCobroServicios servicio;
    public ControladorPoliticasCobroAdmin(ServicioPoliticasCobroServicios servicio) { this.servicio=servicio; }
    @GetMapping public PoliticasCobroServiciosResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) { return servicio.obtener(usuario.empresaId()); }
}
