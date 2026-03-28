package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioInternoPerfilEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UsuarioInternoPerfilRepositorio extends JpaRepository<UsuarioInternoPerfilEntidad, Long> {

    List<UsuarioInternoPerfilEntidad> findByUsuarioIdIn(Collection<Long> usuarioIds);
}
