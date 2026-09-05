package com.techprotech.agenda.modulos.onboarding.api;

import com.techprotech.agenda.modulos.onboarding.api.dto.RegistrarEmpresaRequest;
import com.techprotech.agenda.modulos.onboarding.api.dto.RegistrarEmpresaResponse;
import com.techprotech.agenda.modulos.onboarding.aplicacion.ServicioOnboardingEmpresa;
import com.techprotech.agenda.seguridad.cookies.ServicioCookieTokenActualizacion;
import jakarta.servlet.http.HttpServletResponse;
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
    private final ServicioCookieTokenActualizacion cookies;

    public ControladorOnboardingEmpresa(
            ServicioOnboardingEmpresa servicioOnboardingEmpresa,
            ServicioCookieTokenActualizacion cookies
    ) {
        this.servicioOnboardingEmpresa = servicioOnboardingEmpresa;
        this.cookies = cookies;
    }

    @PostMapping("/empresas")
    public ResponseEntity<RegistrarEmpresaResponse> registrarEmpresa(
            @Valid @RequestBody RegistrarEmpresaRequest request,
            HttpServletResponse response
    ) {
        RegistrarEmpresaResponse registro = servicioOnboardingEmpresa.registrarEmpresa(request);
        cookies.agregar(response, registro.sesion());
        return ResponseEntity.ok(registro);
    }
}
