package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;

import com.techprotech.agenda.modulos.plataforma.api.dto.ResumenPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.TenantPlataformaResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class ConsultaPlataformaRepositorio {
    private final JdbcTemplate jdbc;

    public ConsultaPlataformaRepositorio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ResumenPlataformaResponse resumen() {
        return jdbc.queryForObject("""
                SELECT
                  (SELECT COUNT(*) FROM empresa) tenants_total,
                  (SELECT COUNT(*) FROM empresa WHERE estado = 'ACTIVA') tenants_activos,
                  (SELECT COUNT(*) FROM usuario) usuarios_tenant,
                  (SELECT COUNT(DISTINCT empresa_id) FROM empresa_proveedor_pago WHERE estado = 'CONECTADO') pagos_conectados,
                  COALESCE(SUM(CASE WHEN c.liquidacion_definitiva THEN c.importe_bruto ELSE 0 END), 0) bruto,
                  COALESCE(SUM(CASE WHEN c.liquidacion_definitiva THEN c.comision_proveedor_total ELSE 0 END), 0) comision_proveedor,
                  COALESCE(SUM(CASE WHEN c.liquidacion_definitiva THEN c.comision_plataforma ELSE 0 END), 0) comision_fluora,
                  COALESCE(SUM(CASE WHEN c.liquidacion_definitiva THEN c.importe_neto ELSE 0 END), 0) neto,
                  CASE WHEN COUNT(DISTINCT CASE WHEN c.liquidacion_definitiva THEN c.moneda END) = 1
                       THEN MAX(CASE WHEN c.liquidacion_definitiva THEN c.moneda END) ELSE NULL END moneda
                FROM conciliacion_pago c
                """, (rs, row) -> new ResumenPlataformaResponse(
                rs.getLong("tenants_total"), rs.getLong("tenants_activos"), rs.getLong("usuarios_tenant"),
                rs.getLong("pagos_conectados"), decimal(rs.getBigDecimal("bruto")),
                decimal(rs.getBigDecimal("comision_proveedor")), decimal(rs.getBigDecimal("comision_fluora")),
                decimal(rs.getBigDecimal("neto")), rs.getString("moneda")));
    }

    public List<TenantPlataformaResponse> tenants() {
        return jdbc.query("""
                SELECT e.id, e.nombre, e.slug, e.estado, e.zona_horaria, e.creada_en,
                       COALESCE(s.total, 0) sucursales, COALESCE(u.total, 0) usuarios,
                       CASE WHEN p.empresa_id IS NULL THEN FALSE ELSE TRUE END pagos_conectados,
                       COALESCE(f.bruto, 0) bruto, COALESCE(f.comision_fluora, 0) comision_fluora,
                       COALESCE(f.neto, 0) neto, f.moneda
                FROM empresa e
                LEFT JOIN (SELECT empresa_id, COUNT(*) total FROM sucursal GROUP BY empresa_id) s ON s.empresa_id = e.id
                LEFT JOIN (SELECT empresa_id, COUNT(*) total FROM usuario GROUP BY empresa_id) u ON u.empresa_id = e.id
                LEFT JOIN (SELECT DISTINCT empresa_id FROM empresa_proveedor_pago WHERE estado = 'CONECTADO') p ON p.empresa_id = e.id
                LEFT JOIN (
                  SELECT empresa_id, SUM(importe_bruto) bruto, SUM(COALESCE(comision_plataforma, 0)) comision_fluora,
                         SUM(COALESCE(importe_neto, 0)) neto,
                         CASE WHEN COUNT(DISTINCT moneda) = 1 THEN MAX(moneda) ELSE NULL END moneda
                  FROM conciliacion_pago WHERE liquidacion_definitiva = TRUE GROUP BY empresa_id
                ) f ON f.empresa_id = e.id
                ORDER BY e.creada_en DESC, e.id DESC
                """, (rs, row) -> new TenantPlataformaResponse(
                rs.getLong("id"), rs.getString("nombre"), rs.getString("slug"), rs.getString("estado"),
                rs.getString("zona_horaria"), rs.getTimestamp("creada_en").toLocalDateTime(),
                rs.getLong("sucursales"), rs.getLong("usuarios"), rs.getBoolean("pagos_conectados"),
                decimal(rs.getBigDecimal("bruto")), decimal(rs.getBigDecimal("comision_fluora")),
                decimal(rs.getBigDecimal("neto")), rs.getString("moneda")));
    }

    private static BigDecimal decimal(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2) : valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
