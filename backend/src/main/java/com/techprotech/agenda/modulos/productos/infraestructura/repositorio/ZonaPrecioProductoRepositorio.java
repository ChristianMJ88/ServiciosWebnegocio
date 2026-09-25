package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ZonaPrecioProductoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ZonaPrecioProductoRepositorio extends JpaRepository<ZonaPrecioProductoEntidad, Long> {
    List<ZonaPrecioProductoEntidad> findByEmpresaIdOrderByPrioridadAscNombreAsc(Long empresaId);
    Optional<ZonaPrecioProductoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
    boolean existsByEmpresaIdAndClave(Long empresaId, String clave);
    boolean existsByEmpresaIdAndClaveAndIdNot(Long empresaId, String clave, Long id);
}
