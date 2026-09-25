package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.MovimientoInventarioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepositorio extends JpaRepository<MovimientoInventarioEntidad, Long> {
    List<MovimientoInventarioEntidad> findTop100ByEmpresaIdAndSucursalIdInOrderByCreadoEnDesc(Long empresaId, List<Long> sucursalIds);
}
