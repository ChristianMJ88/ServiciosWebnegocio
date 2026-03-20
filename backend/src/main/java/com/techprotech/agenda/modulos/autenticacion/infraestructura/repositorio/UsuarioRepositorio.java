package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<UsuarioEntidad, Long> {

    Optional<UsuarioEntidad> findByEmpresaIdAndCorreo(Long empresaId, String correo);

    boolean existsByEmpresaIdAndCorreo(Long empresaId, String correo);

    Optional<UsuarioEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    List<UsuarioEntidad> findByEmpresaIdAndIdIn(Long empresaId, Collection<Long> ids);
}
