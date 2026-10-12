package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaDetalleEntidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RentabilidadVentasRepositorio extends JpaRepository<VentaDetalleEntidad, Long> {
    interface Fila {
        Long getProductoId(); String getNombre(); String getSku(); String getMoneda();
        BigDecimal getCantidad(); BigDecimal getVentasNetas(); BigDecimal getCostoConocido();
        Long getLineasSinCosto();
    }
    String FILTRO = " from venta_detalle d join venta v on v.id=d.venta_id and v.empresa_id=d.empresa_id "
        + "where d.empresa_id=:empresaId and v.estado='PAGADA' and d.tipo_concepto='PRODUCTO' "
        + "and v.creada_en>=:desde and v.creada_en<:hasta "
        + "and (:sucursalId is null or v.sucursal_id=:sucursalId) "
        + "and (lower(d.nombre) like concat('%',:busqueda,'%') or lower(coalesce(d.sku,'')) like concat('%',:busqueda,'%')) ";
    @Query(value="select d.producto_id as productoId, max(d.nombre) as nombre, max(d.sku) as sku, v.moneda as moneda, "
        + "sum(d.cantidad) as cantidad, sum(d.total-d.impuestos) as ventasNetas, "
        + "sum(case when d.costo_unitario_historico is not null then d.costo_unitario_historico*d.cantidad else 0 end) as costoConocido, "
        + "sum(case when d.costo_unitario_historico is null then 1 else 0 end) as lineasSinCosto "
        + FILTRO + "group by d.producto_id,v.moneda order by max(d.nombre),d.producto_id,v.moneda",
        countQuery="select count(*) from (select d.producto_id,v.moneda " + FILTRO + "group by d.producto_id,v.moneda) grupos",
        nativeQuery=true)
    Page<Fila> consultar(Long empresaId, LocalDateTime desde, LocalDateTime hasta, Long sucursalId, String busqueda, Pageable pageable);
}
