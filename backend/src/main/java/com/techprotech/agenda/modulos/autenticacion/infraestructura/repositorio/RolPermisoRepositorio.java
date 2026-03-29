package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolPermisoEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolPermisoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RolPermisoRepositorio extends JpaRepository<RolPermisoEntidad, RolPermisoId> {

    @Query("""
            select distinct rp.permiso.codigo
            from UsuarioRolEntidad ur
            join RolPermisoEntidad rp on rp.rol.id = ur.rol.id
            where ur.usuario.id = :usuarioId
            order by rp.permiso.codigo
            """)
    List<String> findCodigosPermisosByUsuarioId(Long usuarioId);

    List<RolPermisoEntidad> findByRol_IdIn(Collection<Long> rolIds);
}
