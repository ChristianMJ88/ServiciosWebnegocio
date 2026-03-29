package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEmpresaPermisoEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEmpresaPermisoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RolEmpresaPermisoRepositorio extends JpaRepository<RolEmpresaPermisoEntidad, RolEmpresaPermisoId> {

    List<RolEmpresaPermisoEntidad> findByRolEmpresa_IdIn(Collection<Long> rolEmpresaIds);

    void deleteByRolEmpresa_Id(Long rolEmpresaId);

    @Query("""
            select distinct rep.permiso.codigo
            from UsuarioRolEmpresaEntidad ure
            join RolEmpresaPermisoEntidad rep on rep.rolEmpresa.id = ure.rolEmpresa.id
            where ure.usuario.id = :usuarioId
              and ure.rolEmpresa.empresaId = :empresaId
              and ure.rolEmpresa.activo = true
            order by rep.permiso.codigo
            """)
    List<String> findCodigosPermisosByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);
}
