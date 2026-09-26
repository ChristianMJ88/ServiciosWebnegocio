package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaDetalleEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface VentaDetalleRepositorio extends JpaRepository<VentaDetalleEntidad, Long> {
    List<VentaDetalleEntidad> findByEmpresaIdAndVentaIdOrderByIdAsc(Long empresaId, Long ventaId);

    @Query("select coalesce(sum(d.total), 0) from VentaDetalleEntidad d, VentaEntidad v where d.ventaId = v.id and d.empresaId = :empresaId and d.citaId = :citaId and v.estado = 'PAGADA'")
    BigDecimal sumarServiciosPagadosPorCita(Long empresaId, Long citaId);
}
