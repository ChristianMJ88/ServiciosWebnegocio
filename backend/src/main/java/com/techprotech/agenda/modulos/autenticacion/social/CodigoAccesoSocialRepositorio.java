package com.techprotech.agenda.modulos.autenticacion.social;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CodigoAccesoSocialRepositorio extends JpaRepository<CodigoAccesoSocialEntidad, Long> {
    Optional<CodigoAccesoSocialEntidad> findByTokenHash(String tokenHash);
}
