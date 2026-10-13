package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.CajaSesionEntidad;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CajaSesionRepositorio extends JpaRepository<CajaSesionEntidad, Long> {

    Optional<CajaSesionEntidad> findByEmpresaIdAndSucursalIdAndEstado(Long empresaId, Long sucursalId, String estado);

    Optional<CajaSesionEntidad> findFirstByEmpresaIdAndEstadoOrderByAbiertaEnDesc(Long empresaId, String estado);

    Optional<CajaSesionEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    /**
     * Bloqueo pesimista para verificar sesión abierta durante apertura.
     * Evita race condition: dos hilos podrían pasar el check y abrir dos sesiones.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CajaSesionEntidad s where s.empresaId = :empresaId and s.sucursalId = :sucursalId and s.estado = :estado")
    Optional<CajaSesionEntidad> findParaAperturaConBloqueo(
            @Param("empresaId") Long empresaId,
            @Param("sucursalId") Long sucursalId,
            @Param("estado") String estado
    );
}