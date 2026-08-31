package com.techprotech.agenda.modulos.onboarding.api;

import com.techprotech.agenda.modulos.onboarding.api.dto.*;
import com.techprotech.agenda.modulos.onboarding.aplicacion.ServicioVerificacionCorreoOnboarding;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publico/onboarding/verificacion-correo")
public class ControladorVerificacionCorreoOnboarding {
    private final ServicioVerificacionCorreoOnboarding servicio;
    public ControladorVerificacionCorreoOnboarding(ServicioVerificacionCorreoOnboarding servicio){this.servicio=servicio;}
    @PostMapping("/confirmar")
    public ResponseEntity<ConfirmarCorreoOnboardingResponse> confirmar(@Valid @RequestBody ConfirmarCorreoOnboardingRequest request){return ResponseEntity.ok(servicio.confirmar(request.token()));}
}
