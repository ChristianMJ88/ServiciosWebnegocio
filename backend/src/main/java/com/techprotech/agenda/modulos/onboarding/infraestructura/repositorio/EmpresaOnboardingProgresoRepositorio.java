package com.techprotech.agenda.modulos.onboarding.infraestructura.repositorio;

import com.techprotech.agenda.modulos.onboarding.infraestructura.entidad.EmpresaOnboardingProgresoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaOnboardingProgresoRepositorio extends JpaRepository<EmpresaOnboardingProgresoEntidad, Long> {
}
