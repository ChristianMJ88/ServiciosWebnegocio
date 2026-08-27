package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmpresaSitioConfigRepositorio extends JpaRepository<EmpresaSitioConfigEntidad, Long> {

    Optional<EmpresaSitioConfigEntidad> findBySlug(String slug);

    Optional<EmpresaSitioConfigEntidad> findByDominioPrincipalIgnoreCase(String dominioPrincipal);

    boolean existsBySlug(String slug);

    @Query("select sitio.slug from EmpresaSitioConfigEntidad sitio where sitio.publicado = true order by sitio.slug")
    List<String> findSlugsPublicados();
}
