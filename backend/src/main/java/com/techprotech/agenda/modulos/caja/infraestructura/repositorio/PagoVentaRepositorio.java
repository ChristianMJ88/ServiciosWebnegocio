package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.PagoVentaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoVentaRepositorio extends JpaRepository<PagoVentaEntidad, Long> {
    List<PagoVentaEntidad> findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(Long empresaId, Long cajaSesionId);
    List<PagoVentaEntidad> findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc(Long empresaId, Long ventaId);
}
