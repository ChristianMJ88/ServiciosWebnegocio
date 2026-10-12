package com.techprotech.agenda.modulos.ventasstaff.infraestructura;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaDetalleEntidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VentasStaffRepositorio extends JpaRepository<VentaDetalleEntidad,Long> {
    interface Fila {
        Long getId(); String getFolio(); String getNombre(); String getConcepto();
        String getEstado(); String getMoneda(); BigDecimal getCantidad(); BigDecimal getTotal(); LocalDateTime getFecha();
    }
    String FILTRO=" from venta_detalle d join venta v on v.id=d.venta_id and v.empresa_id=d.empresa_id "
        +"left join cita c on c.id=d.cita_id and c.empresa_id=d.empresa_id "
        +"where d.empresa_id=:empresaId and coalesce(c.prestador_id,d.prestador_responsable_id)=:usuarioId "
        +"and v.creada_en>=:desde and v.creada_en<:hasta "
        +"and (:concepto='' or d.tipo_concepto=:concepto) "
        +"and (lower(d.nombre) like concat('%',:busqueda,'%') or lower(v.folio) like concat('%',:busqueda,'%')) ";
    @Query(value="select d.id as id,v.folio as folio,d.nombre as nombre,d.tipo_concepto as concepto,v.estado as estado,"
        +"v.moneda as moneda,d.cantidad as cantidad,d.total as total,v.creada_en as fecha"+FILTRO+"order by v.creada_en desc,d.id desc",
        countQuery="select count(*)"+FILTRO,nativeQuery=true)
    Page<Fila> consultar(Long empresaId,Long usuarioId,LocalDateTime desde,LocalDateTime hasta,String concepto,String busqueda,Pageable pageable);
}
