package com.techprotech.agenda.modulos.autenticacion.social;

import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaAccesoApp;
import com.techprotech.agenda.seguridad.cookies.ServicioCookieTokenActualizacion;
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
    private final ServicioAutenticacionSocialMicrosoft microsoft;
    private final ServicioTokenRegistroSocial tokens;
    private final ServicioAccesoSocial accesoSocial;
    private final ServicioCookieTokenActualizacion cookies;

    public ControladorAutenticacionSocial(ServicioAutenticacionSocialGoogle google,
            ServicioAutenticacionSocialMicrosoft microsoft, ServicioTokenRegistroSocial tokens,
            ServicioAccesoSocial accesoSocial, ServicioCookieTokenActualizacion cookies) {
        this.google = google;
        this.microsoft = microsoft;
        this.tokens = tokens;
        this.accesoSocial = accesoSocial;
        this.cookies = cookies;
    }

    @GetMapping("/configuracion")
    public ResponseEntity<ConfiguracionSocialResponse> configuracion() {
        return ResponseEntity.ok(new ConfiguracionSocialResponse(
                google.estaHabilitado(), microsoft.estaHabilitado(), false,
                google.estaHabilitado() ? "/auth/social/google/iniciar" : null,
                google.estaHabilitado() ? "/auth/social/google/iniciar-acceso" : null,
                microsoft.estaHabilitado() ? "/auth/social/microsoft/iniciar" : null,
                microsoft.estaHabilitado() ? "/auth/social/microsoft/iniciar-acceso" : null
        ));
    }

    @PostMapping("/perfil-registro")
    public ResponseEntity<PerfilRegistroSocialResponse> perfilRegistro(@Valid @RequestBody TokenRegistroSocialRequest request) {
        PerfilRegistroSocial perfil = tokens.validarToken(request.token());
        return ResponseEntity.ok(new PerfilRegistroSocialResponse(perfil.proveedor(), perfil.correo(), perfil.nombre()));
    }

    @GetMapping("/google/iniciar")
    public void iniciarGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect(google.construirUrlInicio());
    }

    @GetMapping("/google/iniciar-acceso")
    public void iniciarAccesoGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect(google.construirUrlInicio("ACCESO"));
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

    @GetMapping("/microsoft/iniciar")
    public void iniciarMicrosoft(HttpServletResponse response) throws IOException {
        response.sendRedirect(microsoft.construirUrlInicio("REGISTRO"));
    }

    @GetMapping("/microsoft/iniciar-acceso")
    public void iniciarAccesoMicrosoft(HttpServletResponse response) throws IOException {
        response.sendRedirect(microsoft.construirUrlInicio("ACCESO"));
    }

    @GetMapping("/microsoft/callback")
    public void callbackMicrosoft(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(microsoft.completar(code, state, error));
    }

    @PostMapping("/intercambiar-acceso")
    public ResponseEntity<RespuestaAccesoApp> intercambiarAcceso(
            @Valid @RequestBody IntercambiarAccesoSocialRequest request,
            HttpServletResponse response) {
        RespuestaAccesoApp acceso = accesoSocial.intercambiar(request.codigo(), request.empresaId());
        cookies.agregar(response, acceso);
        return ResponseEntity.ok(acceso);
    }
}
