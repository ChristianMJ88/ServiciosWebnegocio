package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface ProductoRepositorio extends JpaRepository<ProductoEntidad, Long> {
    interface ProductoPublicoIndex {
        String getSitioSlug();
        String getProductoSlug();
    }
    List<ProductoEntidad> findByEmpresaIdOrderByNombreAsc(Long empresaId);
    List<ProductoEntidad> findByEmpresaIdAndActivoTrueAndVisiblePublicoTrueOrderByNombreAsc(Long empresaId);
    Optional<ProductoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
    List<ProductoEntidad> findByEmpresaIdAndIdIn(Long empresaId, Collection<Long> ids);
    Optional<ProductoEntidad> findByEmpresaIdAndSlugAndActivoTrueAndVisiblePublicoTrue(Long empresaId, String slug);
    boolean existsByEmpresaIdAndSku(Long empresaId, String sku);
    boolean existsByEmpresaIdAndSkuAndIdNot(Long empresaId, String sku, Long id);
    boolean existsByEmpresaIdAndSlug(Long empresaId, String slug);
    boolean existsByEmpresaIdAndSlugAndIdNot(Long empresaId, String slug, Long id);
    long countByEmpresaIdAndActivoTrue(Long empresaId);
    long countByEmpresaId(Long empresaId);
    long countByEmpresaIdAndActivoTrueAndVisiblePublicoTrue(Long empresaId);
    @Query("""
      select p from ProductoEntidad p
      where p.empresaId = :empresaId and (:busqueda = '' or lower(p.nombre) like concat('%', :busqueda, '%')
        or lower(p.sku) like concat('%', :busqueda, '%')
        or p.categoriaId in (select c.id from CategoriaProductoEntidad c where c.empresaId = :empresaId and lower(c.nombre) like concat('%', :busqueda, '%'))
        or p.proveedorId in (select v.id from ProveedorProductoEntidad v where v.empresaId = :empresaId and lower(v.nombre) like concat('%', :busqueda, '%')))
      order by p.nombre, p.id
      """)
    org.springframework.data.domain.Page<ProductoEntidad> buscarPagina(@org.springframework.data.repository.query.Param("empresaId") Long empresaId,
      @org.springframework.data.repository.query.Param("busqueda") String busqueda, org.springframework.data.domain.Pageable pageable);


    @Query(value = """
            SELECT sitio.slug AS sitioSlug, producto.slug AS productoSlug
            FROM producto producto
            JOIN empresa_sitio_config sitio ON sitio.empresa_id = producto.empresa_id AND sitio.publicado = TRUE
            JOIN producto_tienda_config tienda ON tienda.empresa_id = producto.empresa_id AND tienda.habilitada = TRUE
            WHERE producto.activo = TRUE AND producto.visible_publico = TRUE
            ORDER BY sitio.slug, producto.slug
            """, nativeQuery = true)
    List<ProductoPublicoIndex> findIndiceProductosPublicados();
}
