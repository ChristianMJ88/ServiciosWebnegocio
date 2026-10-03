package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;

import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.UsuarioPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioPlataformaRepositorio extends JpaRepository<UsuarioPlataformaEntidad, Long> {
    Optional<UsuarioPlataformaEntidad> findByCorreoIgnoreCase(String correo);
}
