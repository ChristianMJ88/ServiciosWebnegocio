package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RecuperacionContrasenaEntidad;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecuperacionContrasenaRepositorio extends JpaRepository<RecuperacionContrasenaEntidad, Long> {
    boolean existsByUsuarioIdAndEstadoAndCreadoEnAfter(Long usuarioId, String estado, LocalDateTime desde);
    List<RecuperacionContrasenaEntidad> findByUsuarioIdAndEstado(Long usuarioId, String estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<RecuperacionContrasenaEntidad> findByTokenHash(String tokenHash);
}
