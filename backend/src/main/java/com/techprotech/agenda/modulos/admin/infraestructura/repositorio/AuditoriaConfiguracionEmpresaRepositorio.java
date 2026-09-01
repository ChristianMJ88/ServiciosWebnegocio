package com.techprotech.agenda.modulos.admin.infraestructura.repositorio;

import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaConfiguracionEmpresaRepositorio extends JpaRepository<AuditoriaConfiguracionEmpresaEntidad, Long> {

    List<AuditoriaConfiguracionEmpresaEntidad> findTop30ByEmpresaIdOrderByCreadoEnDesc(Long empresaId);

    boolean existsByEmpresaIdAndAccion(Long empresaId, String accion);
}
