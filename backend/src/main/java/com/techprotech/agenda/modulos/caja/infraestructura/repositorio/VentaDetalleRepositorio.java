package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaDetalleEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface VentaDetalleRepositorio extends JpaRepository<VentaDetalleEntidad, Long> {

    List<VentaDetalleEntidad> findByEmpresaIdAndVentaIdOrderByIdAsc(Long empresaId, Long ventaId);

    List<VentaDetalleEntidad> findByEmpresaIdAndVentaIdIn(Long empresaId, Collection<Long> ventaIds);


    @Query("select coalesce(sum(d.total), 0) from VentaDetalleEntidad d, VentaEntidad v " +
           "where d.ventaId = v.id and d.empresaId = :empresaId and v.empresaId = :empresaId and d.tipoConcepto = 'SERVICIO' and d.citaId = :citaId and v.estado = 'PAGADA'")
    BigDecimal sumarServiciosPagadosPorCita(Long empresaId, Long citaId);

    /**
     * Suma de detalles de servicio agrupada por cita — reemplaza N llamadas en un loop.
     * Retorna filas [citaId, total].
     */
    @Query("select d.citaId, coalesce(sum(d.total), 0) from VentaDetalleEntidad d, VentaEntidad v " +
           "where d.ventaId = v.id and d.empresaId = :empresaId and v.empresaId = :empresaId and d.tipoConcepto = 'SERVICIO' and d.citaId in :citaIds " +
           "and v.estado = 'PAGADA' group by d.citaId")
    List<Object[]> sumarServiciosPagadosPorCitas(@Param("empresaId") Long empresaId,
                                                 @Param("citaIds") Collection<Long> citaIds);
}
