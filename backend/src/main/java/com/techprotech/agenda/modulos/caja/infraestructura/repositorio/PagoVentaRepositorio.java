package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.PagoVentaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
