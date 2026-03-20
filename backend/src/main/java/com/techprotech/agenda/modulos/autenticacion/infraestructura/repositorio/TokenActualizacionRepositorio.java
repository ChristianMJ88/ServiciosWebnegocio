package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.TokenActualizacionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TokenActualizacionRepositorio extends JpaRepository<TokenActualizacionEntidad, Long> {

    List<TokenActualizacionEntidad> findByUsuarioIdAndRevocadoEnIsNullAndExpiraEnAfter(Long usuarioId, LocalDateTime fecha);
}
