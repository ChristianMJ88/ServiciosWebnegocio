package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UsuarioRolRepositorio extends JpaRepository<UsuarioRolEntidad, UsuarioRolId> {

    List<UsuarioRolEntidad> findByUsuario_Id(Long usuarioId);

    List<UsuarioRolEntidad> findByUsuario_IdIn(Collection<Long> usuarioIds);

    void deleteByUsuario_IdAndIdEmpresaIdAndRol_CodigoIn(Long usuarioId, Long empresaId, Collection<String> codigos);
}
