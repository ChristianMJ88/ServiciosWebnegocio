package com.techprotech.agenda.modulos.autenticacion.social;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth/social")
public class ControladorAutenticacionSocial {
    private final ServicioAutenticacionSocialGoogle google;

    public ControladorAutenticacionSocial(ServicioAutenticacionSocialGoogle google) {
        this.google = google;
    }

    @GetMapping("/configuracion")
    public ResponseEntity<ConfiguracionSocialResponse> configuracion() {
        return ResponseEntity.ok(new ConfiguracionSocialResponse(
                google.estaHabilitado(), false, false,
                google.estaHabilitado() ? "/auth/social/google/iniciar" : null
        ));
    }

    @PostMapping("/perfil-registro")
    public ResponseEntity<PerfilRegistroSocialResponse> perfilRegistro(@Valid @RequestBody TokenRegistroSocialRequest request) {
        PerfilRegistroSocial perfil = google.validarTokenRegistro(request.token());
        return ResponseEntity.ok(new PerfilRegistroSocialResponse(perfil.proveedor(), perfil.correo(), perfil.nombre()));
    }

    @GetMapping("/google/iniciar")
    public void iniciarGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect(google.construirUrlInicio());
    }

    @GetMapping("/google/callback")
    public void callbackGoogle(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(google.completar(code, state, error));
    }
}
