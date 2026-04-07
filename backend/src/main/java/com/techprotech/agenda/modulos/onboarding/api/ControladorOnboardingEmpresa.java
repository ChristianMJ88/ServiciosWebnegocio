package com.techprotech.agenda.modulos.onboarding.api;

import com.techprotech.agenda.modulos.onboarding.api.dto.RegistrarEmpresaRequest;
import com.techprotech.agenda.modulos.onboarding.api.dto.RegistrarEmpresaResponse;
import com.techprotech.agenda.modulos.onboarding.aplicacion.ServicioOnboardingEmpresa;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
public class ControladorOnboardingEmpresa {

    private final ServicioOnboardingEmpresa servicioOnboardingEmpresa;

    public ControladorOnboardingEmpresa(ServicioOnboardingEmpresa servicioOnboardingEmpresa) {
        this.servicioOnboardingEmpresa = servicioOnboardingEmpresa;
    }

    @PostMapping("/empresas")
    public ResponseEntity<RegistrarEmpresaResponse> registrarEmpresa(@Valid @RequestBody RegistrarEmpresaRequest request) {
        return ResponseEntity.ok(servicioOnboardingEmpresa.registrarEmpresa(request));
    }
}
