package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.CajaSesionEntidad;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CajaSesionRepositorio extends JpaRepository<CajaSesionEntidad, Long> {
    @Query("select s.sucursalId from CajaSesionEntidad s where s.empresaId=:empresaId and s.sucursalId in :sucursales and s.estado=:estado")
    java.util.List<Long> buscarSucursalesConSesion(Long empresaId, java.util.Collection<Long> sucursales, String estado);


    Optional<CajaSesionEntidad> findByEmpresaIdAndSucursalIdAndEstado(Long empresaId, Long sucursalId, String estado);

    Optional<CajaSesionEntidad> findFirstByEmpresaIdAndEstadoOrderByAbiertaEnDesc(Long empresaId, String estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CajaSesionEntidad s where s.id=:id and s.empresaId=:empresaId")
    Optional<CajaSesionEntidad> bloquearPorId(Long id, Long empresaId);
    @Query("select count(o)>0 from OrdenPointCajaEntidad o, VentaEntidad v where o.ventaId=v.id and o.empresaId=:empresaId and v.empresaId=:empresaId and v.cajaSesionId=:sesionId and o.estado not in ('APROBADO','REEMBOLSADO','CANCELADO','RECHAZADO','EXPIRADO')")
    boolean tienePointEnCurso(Long empresaId,Long sesionId);
    @Query("select distinct v.folio from OrdenPointCajaEntidad o, VentaEntidad v where o.ventaId=v.id and o.empresaId=:empresaId and v.empresaId=:empresaId and v.cajaSesionId=:sesionId and o.estado not in ('APROBADO','REEMBOLSADO','CANCELADO','RECHAZADO','EXPIRADO')")
    java.util.List<String> foliosPointEnCurso(Long empresaId,Long sesionId);
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
