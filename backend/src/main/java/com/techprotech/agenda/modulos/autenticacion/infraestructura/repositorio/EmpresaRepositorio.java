package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpresaRepositorio extends JpaRepository<EmpresaEntidad, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from EmpresaEntidad e where e.id=:id")
    Optional<EmpresaEntidad> bloquearPorId(Long id);

    Optional<EmpresaEntidad> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
