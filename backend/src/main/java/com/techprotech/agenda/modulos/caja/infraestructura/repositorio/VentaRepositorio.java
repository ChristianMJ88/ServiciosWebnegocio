package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VentaRepositorio extends JpaRepository<VentaEntidad, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from VentaEntidad v where v.id=:id and v.empresaId=:empresaId")
    Optional<VentaEntidad> findConBloqueoByIdAndEmpresaId(Long id,Long empresaId);
    @org.springframework.data.jpa.repository.Query("select count(o)>0 from OrdenPointCajaEntidad o where o.empresaId=:empresaId and o.ventaId=:ventaId and o.id=:ordenId and not exists (select otra.id from OrdenPointCajaEntidad otra where otra.empresaId=:empresaId and otra.ventaId=:ventaId and otra.id>o.id)")
    boolean esOrdenPointVigente(Long empresaId,Long ventaId,Long ordenId);
    List<VentaEntidad> findTop50ByEmpresaIdAndSucursalIdOrderByCreadaEnDesc(Long empresaId, Long sucursalId);
    org.springframework.data.domain.Page<VentaEntidad> findByEmpresaIdAndSucursalIdAndEstadoAndCanalOrderByCreadaEnAsc(Long empresaId,Long sucursalId,String estado,String canal,org.springframework.data.domain.Pageable pageable);
    Optional<VentaEntidad> findByEmpresaIdAndCheckoutOrdenId(Long empresaId, Long checkoutOrdenId);
    boolean existsByEmpresaIdAndCheckoutOrdenId(Long empresaId, Long checkoutOrdenId);
    Optional<VentaEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
}
