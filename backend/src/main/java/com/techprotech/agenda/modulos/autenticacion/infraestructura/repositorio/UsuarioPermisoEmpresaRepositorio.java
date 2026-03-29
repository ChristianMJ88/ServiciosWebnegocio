package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioPermisoEmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioPermisoEmpresaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UsuarioPermisoEmpresaRepositorio extends JpaRepository<UsuarioPermisoEmpresaEntidad, UsuarioPermisoEmpresaId> {

    List<UsuarioPermisoEmpresaEntidad> findByUsuario_Id(Long usuarioId);

    List<UsuarioPermisoEmpresaEntidad> findByUsuario_IdIn(Collection<Long> usuarioIds);

    void deleteByUsuario_Id(Long usuarioId);
}
