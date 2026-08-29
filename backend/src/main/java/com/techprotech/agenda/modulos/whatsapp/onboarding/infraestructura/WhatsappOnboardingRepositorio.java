package com.techprotech.agenda.modulos.whatsapp.onboarding.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface WhatsappOnboardingRepositorio extends JpaRepository<WhatsappOnboardingEntidad, String> {
    Optional<WhatsappOnboardingEntidad> findByEmpresaId(Long empresaId);
    Optional<WhatsappOnboardingEntidad> findByIdAndEmpresaId(String id, Long empresaId);
    List<WhatsappOnboardingEntidad> findTop50ByEstadoInOrderByActualizadoEnAsc(Collection<String> estados);
}
