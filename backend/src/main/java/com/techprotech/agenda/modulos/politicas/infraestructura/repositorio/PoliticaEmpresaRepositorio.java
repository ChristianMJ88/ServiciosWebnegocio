package com.techprotech.agenda.modulos.politicas.infraestructura.repositorio;

import com.techprotech.agenda.modulos.politicas.infraestructura.entidad.PoliticaEmpresaEntidad;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaEmpresaRepositorio extends JpaRepository<PoliticaEmpresaEntidad,Long> {
    Optional<PoliticaEmpresaEntidad> findFirstByEmpresaIdOrderByVersionDesc(Long empresaId);
    Optional<PoliticaEmpresaEntidad> findByEmpresaIdAndVersion(Long empresaId,Integer version);
    Optional<PoliticaEmpresaEntidad> findByEmpresaIdAndId(Long empresaId,Long id);
}
