package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.PrecioZonaProductoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PrecioZonaProductoRepositorio extends JpaRepository<PrecioZonaProductoEntidad, Long> {
    List<PrecioZonaProductoEntidad> findByEmpresaIdAndProductoIdIn(Long empresaId, Collection<Long> productoIds);
    void deleteByEmpresaIdAndProductoId(Long empresaId, Long productoId);
}
