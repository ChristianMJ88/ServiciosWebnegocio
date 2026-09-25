package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProveedorProductoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorProductoRepositorio extends JpaRepository<ProveedorProductoEntidad, Long> {
    List<ProveedorProductoEntidad> findByEmpresaIdOrderByNombreAsc(Long empresaId);
    Optional<ProveedorProductoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
    boolean existsByEmpresaIdAndClave(Long empresaId, String clave);
    boolean existsByEmpresaIdAndClaveAndIdNot(Long empresaId, String clave, Long id);
}
