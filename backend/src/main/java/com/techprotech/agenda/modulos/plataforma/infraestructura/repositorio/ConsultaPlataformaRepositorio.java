package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;

import com.techprotech.agenda.modulos.plataforma.api.dto.ResumenPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.TenantPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.ConfiguracionComercioTenantResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.DetalleTenantPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.IntegracionPagoTenantResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

    public Optional<DetalleTenantPlataformaResponse> tenant(Long empresaId) {
        TenantPlataformaResponse tenant;
        try {
            tenant = jdbc.queryForObject("""
                    SELECT e.id, e.nombre, e.slug, e.estado, e.zona_horaria, e.creada_en,
                           COALESCE((SELECT COUNT(*) FROM sucursal s WHERE s.empresa_id=e.id),0) sucursales,
                           COALESCE((SELECT COUNT(*) FROM usuario u WHERE u.empresa_id=e.id),0) usuarios,
                           EXISTS(SELECT 1 FROM empresa_proveedor_pago ep WHERE ep.empresa_id=e.id AND ep.estado='CONECTADO') pagos_conectados,
                           COALESCE(f.bruto,0) bruto, COALESCE(f.comision_fluora,0) comision_fluora,
                           COALESCE(f.neto,0) neto, f.moneda
                    FROM empresa e
                    LEFT JOIN (
                      SELECT empresa_id, SUM(importe_bruto) bruto, SUM(COALESCE(comision_plataforma,0)) comision_fluora,
                             SUM(COALESCE(importe_neto,0)) neto,
                             CASE WHEN COUNT(DISTINCT moneda)=1 THEN MAX(moneda) ELSE NULL END moneda
                      FROM conciliacion_pago WHERE liquidacion_definitiva=TRUE GROUP BY empresa_id
                    ) f ON f.empresa_id=e.id
                    WHERE e.id=?
                    """, (rs, row) -> new TenantPlataformaResponse(
                    rs.getLong("id"), rs.getString("nombre"), rs.getString("slug"), rs.getString("estado"),
                    rs.getString("zona_horaria"), rs.getTimestamp("creada_en").toLocalDateTime(),
                    rs.getLong("sucursales"), rs.getLong("usuarios"), rs.getBoolean("pagos_conectados"),
                    decimal(rs.getBigDecimal("bruto")), decimal(rs.getBigDecimal("comision_fluora")),
                    decimal(rs.getBigDecimal("neto")), rs.getString("moneda")), empresaId);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
        Object[] operacion = jdbc.queryForObject("""
                SELECT
                  (SELECT COUNT(*) FROM servicio WHERE empresa_id=? AND activo=TRUE) servicios,
                  (SELECT COUNT(*) FROM producto WHERE empresa_id=? AND activo=TRUE) productos,
                  (SELECT COUNT(*) FROM cliente c JOIN usuario u ON u.id=c.usuario_id WHERE u.empresa_id=?) clientes,
                  (SELECT COUNT(*) FROM cita WHERE empresa_id=? AND creada_en >= UTC_TIMESTAMP() - INTERVAL 30 DAY) citas,
                  (SELECT COUNT(*) FROM venta WHERE empresa_id=? AND creada_en >= UTC_TIMESTAMP() - INTERVAL 30 DAY) ventas,
                  COALESCE((SELECT SUM(total) FROM venta WHERE empresa_id=? AND creada_en >= UTC_TIMESTAMP() - INTERVAL 30 DAY),0) importe_ventas,
                  (SELECT CASE WHEN COUNT(DISTINCT moneda)=1 THEN MAX(moneda) ELSE NULL END
                     FROM venta WHERE empresa_id=? AND creada_en >= UTC_TIMESTAMP() - INTERVAL 30 DAY) moneda_ventas
                """, (rs, row) -> new Object[]{rs.getLong("servicios"), rs.getLong("productos"), rs.getLong("clientes"),
                rs.getLong("citas"), rs.getLong("ventas"), decimal(rs.getBigDecimal("importe_ventas")),
                rs.getString("moneda_ventas")}, empresaId, empresaId, empresaId, empresaId, empresaId, empresaId, empresaId);
        ConfiguracionComercioTenantResponse comercio = comercio(empresaId);
        List<IntegracionPagoTenantResponse> integraciones = jdbc.query("""
                SELECT p.id proveedor_id, p.nombre proveedor_nombre, ep.estado, ep.ambiente,
                       ep.conectado_en, ep.ultima_sincronizacion_en
                FROM empresa_proveedor_pago ep
                JOIN proveedor_pago p ON p.id=ep.proveedor_pago_id
                WHERE ep.empresa_id=?
                ORDER BY p.nombre, ep.ambiente
                """, (rs, row) -> new IntegracionPagoTenantResponse(rs.getLong("proveedor_id"),
                rs.getString("proveedor_nombre"), rs.getString("estado"), rs.getString("ambiente"),
                rs.getTimestamp("conectado_en") == null ? null : rs.getTimestamp("conectado_en").toLocalDateTime(),
                rs.getTimestamp("ultima_sincronizacion_en") == null ? null : rs.getTimestamp("ultima_sincronizacion_en").toLocalDateTime()),
                empresaId);
        return Optional.of(new DetalleTenantPlataformaResponse(tenant, (long)operacion[0], (long)operacion[1],
                (long)operacion[2], (long)operacion[3], (long)operacion[4], (BigDecimal)operacion[5],
                (String)operacion[6], comercio, integraciones));
    }

    private ConfiguracionComercioTenantResponse comercio(Long empresaId) {
        List<ConfiguracionComercioTenantResponse> datos = jdbc.query("""
                SELECT cc.empresa_id checkout_id, COALESCE(cc.habilitado,FALSE) checkout_habilitado,
                       COALESCE(cc.permitir_pago_productos,FALSE) productos, COALESCE(cc.permitir_pago_servicios,FALSE) servicios,
                       cc.moneda, ec.empresa_id envio_id, COALESCE(ec.habilitado,FALSE) envio_habilitado,
                       ec.proveedor, ec.ambiente
                FROM empresa e
                LEFT JOIN empresa_checkout_config cc ON cc.empresa_id=e.id
                LEFT JOIN empresa_envio_config ec ON ec.empresa_id=e.id
                WHERE e.id=?
                """, (rs, row) -> new ConfiguracionComercioTenantResponse(
                rs.getObject("checkout_id") != null, rs.getBoolean("checkout_habilitado"),
                rs.getBoolean("productos"), rs.getBoolean("servicios"), rs.getString("moneda"),
                rs.getObject("envio_id") != null, rs.getBoolean("envio_habilitado"),
                rs.getString("proveedor"), rs.getString("ambiente")), empresaId);
        return datos.isEmpty() ? new ConfiguracionComercioTenantResponse(false, false, false, false,
                null, false, false, null, null) : datos.getFirst();
    }

    private static BigDecimal decimal(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2) : valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
