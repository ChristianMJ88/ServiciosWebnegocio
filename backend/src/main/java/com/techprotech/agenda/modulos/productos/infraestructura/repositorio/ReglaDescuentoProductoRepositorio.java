package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ReglaDescuentoProductoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReglaDescuentoProductoRepositorio extends JpaRepository<ReglaDescuentoProductoEntidad, Long> {
    List<ReglaDescuentoProductoEntidad> findByEmpresaIdOrderByPrioridadDescNombreAsc(Long empresaId);
    Optional<ReglaDescuentoProductoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
}
