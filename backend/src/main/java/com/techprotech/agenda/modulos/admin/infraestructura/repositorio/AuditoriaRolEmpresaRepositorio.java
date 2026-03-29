package com.techprotech.agenda.modulos.admin.infraestructura.repositorio;

import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaRolEmpresaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRolEmpresaRepositorio extends JpaRepository<AuditoriaRolEmpresaEntidad, Long> {

    List<AuditoriaRolEmpresaEntidad> findTop30ByEmpresaIdOrderByCreadoEnDesc(Long empresaId);
}
