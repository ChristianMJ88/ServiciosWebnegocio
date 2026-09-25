package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.CategoriaProductoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaProductoRepositorio extends JpaRepository<CategoriaProductoEntidad, Long> {
    List<CategoriaProductoEntidad> findByEmpresaIdOrderByOrdenAscNombreAsc(Long empresaId);
    Optional<CategoriaProductoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
    boolean existsByEmpresaIdAndSlug(Long empresaId, String slug);
    boolean existsByEmpresaIdAndSlugAndIdNot(Long empresaId, String slug, Long id);
}
