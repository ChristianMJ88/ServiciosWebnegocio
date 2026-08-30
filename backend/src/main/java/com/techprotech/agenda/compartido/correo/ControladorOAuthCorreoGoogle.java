package com.techprotech.agenda.compartido.correo;

import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/correo/oauth/google")
public class ControladorOAuthCorreoGoogle {
    private final ServicioOAuthCorreoGoogle servicio;
    public ControladorOAuthCorreoGoogle(ServicioOAuthCorreoGoogle servicio) { this.servicio = servicio; }

    @GetMapping("/iniciar")
    @PreAuthorize("hasAuthority('CONFIGURACION_EMPRESA_GESTIONAR')")
    public InicioOAuthCorreoMicrosoftResponse iniciar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.iniciar(usuario.empresaId(), usuario.usuarioId());
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam String state,
                                         @RequestParam(required = false) String error) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(servicio.completar(code, state, error)).toString()).build();
    }
}
