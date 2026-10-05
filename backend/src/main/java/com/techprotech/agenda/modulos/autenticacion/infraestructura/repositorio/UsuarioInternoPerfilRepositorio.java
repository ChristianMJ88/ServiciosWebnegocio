package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioInternoPerfilEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UsuarioInternoPerfilRepositorio extends JpaRepository<UsuarioInternoPerfilEntidad, Long> {

    List<UsuarioInternoPerfilEntidad> findByUsuarioIdIn(Collection<Long> usuarioIds);

    @Query("""
            select count(perfil)
            from UsuarioInternoPerfilEntidad perfil
            join UsuarioEntidad usuario on usuario.id = perfil.usuarioId
            where usuario.empresaId = :empresaId and usuario.habilitado = true
            """)
    long countActivosByEmpresaId(@Param("empresaId") Long empresaId);
}
