# Rentabilidad del catálogo

La pestaña Rentabilidad de /admin/productos consulta GET /api/v1/admin/productos/rentabilidad. La empresa procede exclusivamente del usuario autenticado; aplica el permiso de gestión de Productos y la habilitación del módulo existente. Búsqueda y paginación se ejecutan en backend.

El reporte inicial muestra precio base, costo actual, diferencia (precio menos costo) y margen (diferencia / precio × 100). Conserva valores negativos; sin costo o con precio cero no inventa un porcentaje. No suma monedas distintas.

Es una estimación comercial, no utilidad bruta contable ni ganancia neta. No incluye descuentos, precios por zona, impuestos ni comisiones. No debe recalcular ventas históricas con el costo actual: las ventas anteriores a V120 no conservan costo unitario histórico y Caja actualmente guarda impuestos en cero. Para un reporte realizado confiable queda pendiente definir y persistir el costo de venta, política de impuestos y devoluciones por línea en todos los canales, incluyendo checkout, y conciliar comisiones sin duplicarlas por artículo.

## Captura del costo histórico

V120 añade costo_unitario_historico nullable en venta_detalle y checkout_orden_detalle. Caja toma el costo del producto al registrar la venta; checkout lo toma al crear la orden usando producto y empresa, y copia ese mismo valor al proyectar el pago aprobado, sin consultar el costo actual. La proyección mantiene su protección contra duplicados. El DTO de venta expone costoUnitarioHistorico. Servicios y productos sin costo conservan NULL. No hay backfill de ventas ni órdenes previas. Este cambio permite construir el reporte realizado; todavía no cambia el reporte estimado ni define reglas fiscales nuevas.

## Utilidad realizada

GET /api/v1/admin/productos/rentabilidad/realizada requiere desde y hasta (fecha ISO) y admite busqueda, pagina y sucursalId. Filtra ventas PAGADA y líneas PRODUCTO; el join exige la misma empresa en venta y detalle. Agrupa por producto y moneda, calcula en SQL cantidad, ingreso menos impuestos registrados, costo histórico por cantidad y líneas sin costo. La paginación y el conteo son del servidor. No suma monedas diferentes. Si cualquier línea carece de costo, no publica costo total, UB ni margen para ese grupo; mantiene ingresos y cuenta de líneas incompletas. Utiliza la fecha de venta registrada (en checkout puede ser fecha de orden). No representa ganancia neta: comisiones y devoluciones no registradas quedan fuera. La interfaz permite alternar estimación y realizada, con calendario de periodo y búsqueda.

## Etiquetas públicas y descuentos

V122 añade dias_nuevo en producto_tienda_config (0 desactiva la automatización) y nuevo_desde/nuevo_hasta opcionales en producto. El backend calcula Nuevo desde creado_en por los días configurados; ambas fechas del producto reemplazan ese plazo y son inclusivas. Editar el producto no reinicia la fecha de registro. Las fechas incompletas o invertidas se rechazan.

El catálogo público admite sucursalId, valida que pertenezca al tenant y esté activa, y devuelve sucursales y sucursalSeleccionadaId. Con una única sucursal activa la selecciona el backend. Sin contexto de sucursal no anuncia descuentos exclusivos de una sede. El stock mostrado se limita a la sede elegida. Catálogo y checkout comparten CalculadorDescuentosProductos, incluyendo prioridad, alcance, moneda, combinación, cantidad mínima y vigencia. El catálogo evalúa una unidad; descuentos por cantidad se aplican al cotizar la cantidad requerida. Se conserva sucursalId en enlaces de producto y checkout; la cotización de pago siempre vuelve a calcular en backend.
