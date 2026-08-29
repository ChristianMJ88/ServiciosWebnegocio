package com.techprotech.agenda.modulos.whatsapp.onboarding.api;

import com.techprotech.agenda.modulos.whatsapp.onboarding.aplicacion.ServicioOnboardingWhatsapp;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/whatsapp/onboarding")
@PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
public class ControladorOnboardingWhatsapp {

    private final ServicioOnboardingWhatsapp servicio;

    public ControladorOnboardingWhatsapp(ServicioOnboardingWhatsapp servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/estado")
    public OnboardingWhatsappResponse estado(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.estado(usuario.empresaId());
    }

    @PostMapping("/iniciar")
    public OnboardingWhatsappResponse iniciar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody IniciarOnboardingWhatsappRequest request
    ) {
        return servicio.iniciar(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PostMapping("/completar")
    public OnboardingWhatsappResponse completar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CompletarOnboardingWhatsappRequest request
    ) {
        return servicio.completar(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PostMapping("/reintentar")
    public OnboardingWhatsappResponse reintentar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.reintentar(usuario.empresaId(), usuario.usuarioId());
    }
}
