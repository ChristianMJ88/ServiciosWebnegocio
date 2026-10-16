package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.PagoVentaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PagoVentaRepositorio extends JpaRepository<PagoVentaEntidad, Long> {

    List<PagoVentaEntidad> findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(Long empresaId, Long cajaSesionId);

    @Query("select p from PagoVentaEntidad p, VentaEntidad v " +
            "where p.ventaId = v.id and p.empresaId = :empresaId and p.cajaSesionId = :cajaSesionId " +
            "and v.empresaId = :empresaId and v.estado = 'PAGADA' order by p.registradoEn desc")
    List<PagoVentaEntidad> findPagosVigentesDeSesion(
            @Param("empresaId") Long empresaId,
            @Param("cajaSesionId") Long cajaSesionId
    );

    List<PagoVentaEntidad> findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc(Long empresaId, Long ventaId);

    /** Carga pagos de múltiples ventas en una sola query — evita N+1 en listar(). */
    @Query("select p from PagoVentaEntidad p where p.empresaId = :empresaId and p.ventaId in :ventaIds order by p.ventaId, p.registradoEn desc, p.id desc")
    List<PagoVentaEntidad> findByEmpresaIdAndVentaIdIn(
            @Param("empresaId") Long empresaId,
            @Param("ventaIds") Collection<Long> ventaIds
    );

    @Query("select new com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoConciliadoCita(" +
            "d.id, d.citaId, p.cajaSesionId, d.total, p.metodoPago, p.referencia, " +
            "'Cobro conciliado desde la venta', p.registradoEn) " +
            "from PagoVentaEntidad p, VentaEntidad v, VentaDetalleEntidad d " +
            "where p.ventaId = v.id and d.ventaId = v.id " +
            "and p.empresaId = :empresaId and v.empresaId = :empresaId and d.empresaId = :empresaId " +
            "and d.citaId = :citaId and v.estado = 'PAGADA' order by p.registradoEn desc")
    List<PagoConciliadoCita> findPagosConciliadosPorCita(
            @Param("empresaId") Long empresaId,
            @Param("citaId") Long citaId
    );
}
