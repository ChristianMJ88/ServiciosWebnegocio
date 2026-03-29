package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioInternoSucursalEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioInternoSucursalId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UsuarioInternoSucursalRepositorio extends JpaRepository<UsuarioInternoSucursalEntidad, UsuarioInternoSucursalId> {

    List<UsuarioInternoSucursalEntidad> findByUsuario_Id(Long usuarioId);

    List<UsuarioInternoSucursalEntidad> findByUsuario_IdIn(Collection<Long> usuarioIds);

    void deleteByUsuario_Id(Long usuarioId);
}
