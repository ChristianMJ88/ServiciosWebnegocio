package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenPointCajaRepositorio extends JpaRepository<OrdenPointCajaEntidad, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from OrdenPointCajaEntidad o where o.empresaId=:empresaId and o.publicId=:publicId")
    Optional<OrdenPointCajaEntidad> bloquear(Long empresaId, String publicId);
    @org.springframework.data.jpa.repository.Query("select count(o)>0 from OrdenPointCajaEntidad o where o.empresaId=:empresaId and o.terminalId=:terminalId and o.estado not in ('APROBADO','REEMBOLSADO','CANCELADO','RECHAZADO','EXPIRADO')")
    boolean existeEnCurso(Long empresaId, Long terminalId);
    Optional<OrdenPointCajaEntidad> findByEmpresaIdAndPublicId(Long empresaId, String publicId);
    Optional<OrdenPointCajaEntidad> findByProveedorOrdenId(String proveedorOrdenId);
    Optional<OrdenPointCajaEntidad> findByEmpresaIdAndIdempotencia(Long empresaId, String idempotencia);
    List<OrdenPointCajaEntidad> findTop50ByEmpresaIdOrderByCreadoEnDesc(Long empresaId);
    List<OrdenPointCajaEntidad> findTop50ByEmpresaIdAndTerminalIdInOrderByCreadoEnDesc(Long empresaId,java.util.Set<Long> terminalId);
    @org.springframework.data.jpa.repository.Query("select o from OrdenPointCajaEntidad o where o.empresaId=:empresaId and o.terminalId in :terminales and o.estado not in ('APROBADO','REEMBOLSADO','CANCELADO','RECHAZADO','EXPIRADO') order by o.creadoEn asc")
    List<OrdenPointCajaEntidad> noFinales(Long empresaId,java.util.Set<Long> terminales);
}
