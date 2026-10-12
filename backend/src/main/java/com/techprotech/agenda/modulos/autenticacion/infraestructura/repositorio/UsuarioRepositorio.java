package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<UsuarioEntidad, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UsuarioEntidad u where u.id = :id and u.empresaId = :empresaId")
    Optional<UsuarioEntidad> bloquearPorIdYEmpresa(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("empresaId") Long empresaId);

    Optional<UsuarioEntidad> findByEmpresaIdAndNombreUsuario(Long empresaId, String nombreUsuario);

    Optional<UsuarioEntidad> findByEmpresaIdAndCorreo(Long empresaId, String correo);

    List<UsuarioEntidad> findByCorreoOrderByEmpresaIdAsc(String correo);

    boolean existsByEmpresaIdAndCorreo(Long empresaId, String correo);

    Optional<UsuarioEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    List<UsuarioEntidad> findByEmpresaIdAndIdIn(Long empresaId, Collection<Long> ids);

    List<UsuarioEntidad> findByEmpresaIdOrderByCorreoAsc(Long empresaId);
}
