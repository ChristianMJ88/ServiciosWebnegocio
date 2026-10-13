-- Índice faltante para búsquedas de pagos por venta individual.
-- Necesario para: PagoVentaRepositorio.findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc
-- y para el batch query findByEmpresaIdAndVentaIdIn usado en listar() ventas.
CREATE INDEX idx_pago_venta_empresa_venta ON pago_venta (empresa_id, venta_id, registrado_en);