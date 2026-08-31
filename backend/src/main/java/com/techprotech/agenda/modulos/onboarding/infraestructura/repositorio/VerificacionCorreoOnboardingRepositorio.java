package com.techprotech.agenda.modulos.onboarding.infraestructura.repositorio;

import com.techprotech.agenda.modulos.onboarding.infraestructura.entidad.VerificacionCorreoOnboardingEntidad;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VerificacionCorreoOnboardingRepositorio extends JpaRepository<VerificacionCorreoOnboardingEntidad, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VerificacionCorreoOnboardingEntidad v where v.tokenHash = :hash")
    Optional<VerificacionCorreoOnboardingEntidad> buscarParaConfirmar(@Param("hash") String hash);
}
