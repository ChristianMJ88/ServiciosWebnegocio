package com.techprotech.agenda.modulos.plataforma.api;
import com.techprotech.agenda.modulos.plataforma.api.dto.CorreoFacturacionRequest;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioContactoSuscripcion;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/admin/suscripcion/correo")
@PreAuthorize("hasAuthority('SUSCRIPCION_EMPRESA_GESTIONAR')")
public class ControladorCorreoSuscripcion {
    private final ServicioContactoSuscripcion servicio;
    public ControladorCorreoSuscripcion(ServicioContactoSuscripcion servicio){this.servicio=servicio;}
    public record ContactoResponse(String correo){}
    @GetMapping public ContactoResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario){return new ContactoResponse(servicio.obtener(usuario.empresaId()));}
    @PutMapping public void guardar(@AuthenticationPrincipal UsuarioAutenticado usuario,@Valid @RequestBody CorreoFacturacionRequest solicitud){servicio.guardar(usuario.empresaId(),solicitud.correo());}
}
