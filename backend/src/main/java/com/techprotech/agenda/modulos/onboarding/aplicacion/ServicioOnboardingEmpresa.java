package com.techprotech.agenda.modulos.onboarding.aplicacion;

import com.techprotech.agenda.modulos.onboarding.api.dto.RegistrarEmpresaRequest;
import com.techprotech.agenda.modulos.onboarding.api.dto.RegistrarEmpresaResponse;

public interface ServicioOnboardingEmpresa {

    RegistrarEmpresaResponse registrarEmpresa(RegistrarEmpresaRequest request);
}
