package com.techprotech.agenda.modulos.admin.infraestructura.repositorio;

import com.techprotech.agenda.modulos.admin.infraestructura.entidad.InvitacionUsuarioEmpresaEntidad;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface InvitacionUsuarioEmpresaRepositorio extends JpaRepository<InvitacionUsuarioEmpresaEntidad, Long> {
    List<InvitacionUsuarioEmpresaEntidad> findByEmpresaIdAndCorreoAndEstado(Long empresaId, String correo, String estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvitacionUsuarioEmpresaEntidad> findByTokenHash(String tokenHash);

    Optional<InvitacionUsuarioEmpresaEntidad> findFirstByTokenHash(String tokenHash);
}
