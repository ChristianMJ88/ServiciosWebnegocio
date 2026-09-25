package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepositorio extends JpaRepository<ProductoEntidad, Long> {
    List<ProductoEntidad> findByEmpresaIdOrderByNombreAsc(Long empresaId);
    Optional<ProductoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
    boolean existsByEmpresaIdAndSku(Long empresaId, String sku);
    boolean existsByEmpresaIdAndSkuAndIdNot(Long empresaId, String sku, Long id);
    boolean existsByEmpresaIdAndSlug(Long empresaId, String slug);
    boolean existsByEmpresaIdAndSlugAndIdNot(Long empresaId, String slug, Long id);
}
