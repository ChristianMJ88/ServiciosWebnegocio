package com.techprotech.agenda.modulos.onboarding.api;

import com.techprotech.agenda.modulos.onboarding.api.dto.EstadoOnboardingResponse;
import com.techprotech.agenda.modulos.onboarding.aplicacion.ServicioEstadoOnboarding;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding/estado")
public class ControladorEstadoOnboarding {
    private final ServicioEstadoOnboarding servicio;

    public ControladorEstadoOnboarding(ServicioEstadoOnboarding servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResponseEntity<EstadoOnboardingResponse> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(servicio.obtener(usuario.empresaId(), usuario.usuarioId()));
    }

    @PostMapping("/reenviar-confirmacion")
    public ResponseEntity<Void> reenviar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicio.reenviarConfirmacion(usuario.empresaId(), usuario.usuarioId());
        return ResponseEntity.noContent().build();
    }
}
