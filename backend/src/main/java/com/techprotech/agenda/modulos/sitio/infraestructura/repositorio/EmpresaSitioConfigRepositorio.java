package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpresaSitioConfigRepositorio extends JpaRepository<EmpresaSitioConfigEntidad, Long> {

    Optional<EmpresaSitioConfigEntidad> findBySlug(String slug);

    Optional<EmpresaSitioConfigEntidad> findByDominioPrincipalIgnoreCase(String dominioPrincipal);

    boolean existsBySlug(String slug);
}
