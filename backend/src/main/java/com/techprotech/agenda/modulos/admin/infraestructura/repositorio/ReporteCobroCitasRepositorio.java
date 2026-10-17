package com.techprotech.agenda.modulos.admin.infraestructura.repositorio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteCobroCitasRepositorio {
    private final NamedParameterJdbcTemplate jdbc;
    public ReporteCobroCitasRepositorio(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    /** Solo agregados: no carga citas, clientes, pagos o ventas en memoria. Fechas de cobro en UTC. */
    public List<Fila> consultar(Long empresa, Long sucursal, LocalDateTime desde, LocalDateTime hasta,
            LocalDateTime desdeUtc, LocalDateTime hastaUtc) {
        var p=new MapSqlParameterSource().addValue("empresa",empresa).addValue("sucursal",sucursal);
        String agenda="", caja="", pagoVenta="";
        if(desde!=null) {
            p.addValue("desde",desde).addValue("hasta",hasta).addValue("desdeUtc",desdeUtc).addValue("hastaUtc",hastaUtc);
            agenda=" and c.inicio >= :desde and c.inicio < :hasta";
            caja=" and p.registrado_en >= :desdeUtc and p.registrado_en < :hastaUtc";
            pagoVenta="p.registrado_en >= :desdeUtc and p.registrado_en < :hastaUtc";
        } else pagoVenta="1=1";
        // El prorrateo atribuye al servicio su parte de cada pago de una venta mixta,
        // sin repetir d.total por cada método de pago o fecha de cobro.
        String sql="""
            select c.moneda, sum(case when c.estado in ('PENDIENTE','CONFIRMADA','FINALIZADA') then c.precio else 0 end) programado,
                sum(case when c.estado='FINALIZADA' then c.precio else 0 end) devengado, 0 cobrado, 0 pendiente
            from cita c where c.empresa_id=:empresa and c.sucursal_id=:sucursal
            """+agenda+" group by c.moneda union all "+"""
            select c.moneda, 0, 0, sum(p.monto), 0 from pago_cita p join cita c on c.id=p.cita_id and c.empresa_id=p.empresa_id
            where p.empresa_id=:empresa and c.sucursal_id=:sucursal
            """+caja+" group by c.moneda union all "+"""
            select v.moneda, 0, 0, sum(d.total * pagos.en_periodo / nullif(pagos.total,0)), 0
            from venta v join venta_detalle d on d.venta_id=v.id and d.empresa_id=v.empresa_id
            join (select p.venta_id, p.empresa_id, sum(p.monto) total, sum(case when
            """+pagoVenta+"""
             then p.monto else 0 end) en_periodo from pago_venta p where p.empresa_id=:empresa
             group by p.venta_id,p.empresa_id) pagos on pagos.venta_id=v.id and pagos.empresa_id=v.empresa_id
            join cita c on c.id=d.cita_id and c.empresa_id=d.empresa_id
            where v.empresa_id=:empresa and c.sucursal_id=:sucursal and v.estado='PAGADA' and d.tipo_concepto='SERVICIO'
            group by v.moneda union all
            select c.moneda, 0, 0, 0, sum(greatest(c.precio
                - coalesce((select sum(p.monto) from pago_cita p where p.empresa_id=c.empresa_id and p.cita_id=c.id),0)
                - coalesce((select sum(d.total) from venta_detalle d join venta v on v.id=d.venta_id and v.empresa_id=d.empresa_id
                    where d.empresa_id=c.empresa_id and d.cita_id=c.id and d.tipo_concepto='SERVICIO' and v.estado='PAGADA'),0),0))
            from cita c where c.empresa_id=:empresa and c.sucursal_id=:sucursal
                and c.estado in ('PENDIENTE','CONFIRMADA','FINALIZADA') and (c.estado='FINALIZADA' or c.check_in_en is not null)
            group by c.moneda
            """;
        return jdbc.query(sql,p,(r,n)->new Fila(r.getString(1),r.getBigDecimal(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getBigDecimal(5)));
    }
    public record Fila(String moneda,BigDecimal programado,BigDecimal devengado,BigDecimal cobrado,BigDecimal pendiente) {}
}
