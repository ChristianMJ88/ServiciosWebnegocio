package com.techprotech.agenda.compartido.media.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpresaArchivoMediaRepositorio extends JpaRepository<EmpresaArchivoMediaEntidad, Long> {
    Optional<EmpresaArchivoMediaEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
}
