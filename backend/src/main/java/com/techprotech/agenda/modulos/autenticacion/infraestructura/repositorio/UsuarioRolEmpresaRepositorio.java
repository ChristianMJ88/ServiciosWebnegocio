package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEmpresaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UsuarioRolEmpresaRepositorio extends JpaRepository<UsuarioRolEmpresaEntidad, UsuarioRolEmpresaId> {

    List<UsuarioRolEmpresaEntidad> findByUsuario_Id(Long usuarioId);

    List<UsuarioRolEmpresaEntidad> findByUsuario_IdAndRolEmpresa_EmpresaId(Long usuarioId, Long empresaId);

    List<UsuarioRolEmpresaEntidad> findByUsuario_IdIn(Collection<Long> usuarioIds);

    List<UsuarioRolEmpresaEntidad> findByUsuario_IdInAndRolEmpresa_EmpresaId(Collection<Long> usuarioIds, Long empresaId);

    List<UsuarioRolEmpresaEntidad> findByRolEmpresa_Id(Long rolEmpresaId);

    long countByRolEmpresa_Id(Long rolEmpresaId);

    @Query("""
            select ure.rolEmpresa.id as rolEmpresaId, count(ure) as total
            from UsuarioRolEmpresaEntidad ure
            where ure.rolEmpresa.id in :rolEmpresaIds
            group by ure.rolEmpresa.id
            """)
    List<ConteoUsuariosPorRol> contarUsuariosPorRol(@Param("rolEmpresaIds") Collection<Long> rolEmpresaIds);

    boolean existsByUsuario_IdAndRolEmpresa_Id(Long usuarioId, Long rolEmpresaId);

    void deleteByRolEmpresa_Id(Long rolEmpresaId);

    void deleteByUsuario_IdAndRolEmpresa_EmpresaIdAndRolEmpresa_IdIn(Long usuarioId, Long empresaId, Collection<Long> rolEmpresaIds);

    interface ConteoUsuariosPorRol {
        Long getRolEmpresaId();
        long getTotal();
    }
}
