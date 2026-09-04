package com.techprotech.agenda.modulos.parametros.infraestructura.repositorio;
import com.techprotech.agenda.modulos.parametros.infraestructura.entidad.ParametroSistemaEmpresaEntidad;
import com.techprotech.agenda.modulos.parametros.infraestructura.entidad.ParametroSistemaEmpresaId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ParametroSistemaEmpresaRepositorio extends JpaRepository<ParametroSistemaEmpresaEntidad, ParametroSistemaEmpresaId> {
    List<ParametroSistemaEmpresaEntidad> findByIdEmpresaId(Long empresaId);
}
