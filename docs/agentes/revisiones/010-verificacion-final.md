# Verificación final 010: correcciones de M1/M2 y B1n–B5n

Revisé el código actual en modo de solo lectura. No ejecuté pruebas ni la interfaz y no modifiqué nada. Las cifras de las suites y la revisión visual son las que reporta Codex. No amplié la revisión a 011 ni a 007-2.

**Resultado:** no hay bloqueantes. Encontré **un hallazgo medio de cobertura** (V1). Las demás correcciones cumplen lo pedido en `revisiones/010-cierre.md`. Hay tres observaciones bajas para seguimiento.

## Medio

### V1 · Ninguna prueba demuestra que un reembolso legítimo sigue revirtiendo la venta
**Evidencia**
- `marcarReembolsadaPoint` (`ServicioVentasCaja.java:333-351`) ahora solo revierte si la venta está `PAGADA` y existe un pago Point con la referencia `proveedorPagoId ?: proveedorOrdenId` (`:337-339`).
- La única prueba que la llama directamente comprueba el caso negativo `isFalse()` (`CobrosConcurrentesMysqlTest.java:306`). `PointCreacionSeguraTest.java:90` solo cubre el reparto mixto con `never()`. Busqué `"REEMBOLSADA"` y `marcarReembolsadaPoint` en `backend/src/test`: no hay ningún caso que devuelva `true`.
- La referencia de comparación sale de un campo distinto al que se guardó al confirmar:
  - Al confirmar se usa `remota.pagoId()` de esa respuesta, o `ordenId` si no viene (`ServicioMercadoPagoPoint.java:296`).
  - Al reembolsar se usa `orden.getProveedorPagoId()`, que se actualiza con cualquier respuesta que traiga `pagoId` (`:286`).
  - Si la aprobación llegó sin `payments[0].id` y la respuesta del reembolso sí lo trae, las referencias no coinciden. Entonces un reembolso legítimo, incluido el del botón «Reembolsar», queda en `REVISAR`: la venta sigue `PAGADA`, la cita no vuelve a deber y el corte cuenta dinero ya devuelto.

**Impacto:** si falla, el error queda del lado seguro (revisión, no una reversión indebida). Aun así, el camino principal cambió y nada lo protege contra regresiones.

**Corrección propuesta**
- En `:338-339`, aceptar coincidencia con `proveedorPagoId` **o** con `proveedorOrdenId`. Los dos son de esa misma orden, así que se mantiene la garantía de M1.
- Agregar en `CobrosConcurrentesMysqlTest` una prueba con venta de Caja `PAGADA` + producto + orden Point acreditada y reembolso. Debe comprobar:
  - `true` y venta `REEMBOLSADA`;
  - saldo pendiente igual al precio;
  - una sola llamada a `reintegrarVenta`;
  - una segunda notificación `REEMBOLSADO` que no reintegra, por la guarda `:289`.
- Agregar una variante con aprobación sin `pagoId` y reembolso con `pagoId`.

## Verificado sin hallazgos bloqueantes ni medios

### M1
- `marcarReembolsadaPoint` recibe la orden, bloquea la venta (`findConBloqueo`), exige `PAGADA` y un pago Point con la referencia de la orden. En otro caso anota la revisión, guarda la venta, devuelve `false` y **no** reintegra (`:340-342`).
- `aplicarEstado` pone `REVISAR:` en el detalle cuando recibe `false` (`ServicioMercadoPagoPoint.java:302`). Un reparto previsto inválido se trata como no simple y va a `revisarReembolsoPoint` (`:301,303`).
- Prueba MySQL `aprobacionTardiaNoAcreditaCuentaCanceladaNiOrdenStaffSustituida` (`:288-313`):
  - **Staff:** A tardía, B pagada y reembolso de A. La venta queda `PAGADA`, hay un solo pago `PAY_B` de 700 y el saldo queda en 0.
  - **Caja:** cancelada + producto + reembolso. La venta queda `CANCELADA`, sin pagos.
  - En los dos casos se comprueba `reintegrarVenta` exactamente una vez (la de `cancelarPoint`).
- **Límite:** la prueba verifica pagos y saldo, pero no ejecuta la consulta del resumen del corte (`ServicioCaja.java:412`). La conservación de B en el corte se deduce de que `pago_venta` no cambia.

### M2
- `listarOrdenesCaja` (`ServicioMercadoPagoPoint.java:95-101`) reúne las terminales de la sucursal sin filtrar por activas. Primero devuelve **todas** las `noFinales` y después completa con las 50 más recientes **ya restringidas a esas terminales**, sin duplicar (`LinkedHashMap`, `putIfAbsent`).
- La consulta `noFinales` (`OrdenPointCajaRepositorio.java:20-21`) no tiene límite y usa la misma lista de estados finales que `existeEnCurso` y `tienePointEnCurso`.
- Actividad recorre todo `historialPoint()` sin `slice` (`caja-sales-section.component.html:62`). El `slice(0,5)` que queda en `:59` es del historial de ventas, no de Point.
- El 409 del cierre lista los folios (`ServicioCaja.java:180-181`, `CajaSesionRepositorio.java:26-27`).
- Pruebas:
  - MySQL: `noFinales` incluye la orden en `REQUIERE_REVISION` con seis canceladas (`CobrosConcurrentesMysqlTest.java:239-240`).
  - Spec: seis órdenes finales y una en revisión (`caja-sales-section.component.spec.ts:21`).
- **Límite:**
  - Ninguna prueba recorre `listarOrdenesCaja` con más de 50 órdenes finales más nuevas ni con terminales de otra sucursal.
  - La spec carga `historialPoint` directamente, así que no prueba el orden que devuelve el backend.

### B1n–B5n
- **B1n:** `monedaRespuesta` busca `currency` y luego `currency_id`, primero en la orden y luego en `payments[0]` (`ClienteMercadoPagoPoint.java:98-100`). No se ha certificado con una respuesta real de Mercado Pago. Revisé el código, no la documentación del proveedor.
- **B2n:** `PagosPrevistosPoint.leer` convierte cualquier error en `IllegalStateException` (`:44`). Al aprobar se captura: se registra solo Point y queda la marca de revisión (`ServicioMercadoPagoPoint.java:295-297`), sin propagar la excepción al webhook.
- **B3n:**
  - `mapearPagos` usa `distinct().count()>1` (`ServicioVentasCaja.java:523`), igual que el HQL.
  - La prueba MySQL cubre dos citas, `MIXTO` y el desempate por id en el mismo instante (`CobrosConcurrentesMysqlTest.java:236-238`).
- **B4n:**
  - Los errores leen primero `mensaje` y después `detail` (`caja-sales-section.component.ts:142-231`).
  - `puedeReembolsar` lo calcula el backend: sin reparto, `APROBADO` y sin `REVISAR:` (`ServicioMercadoPagoPoint.java:323`).
- **B5n:**
  - El formulario se reinicia cuando cambia `orden.id` (`caja-cobro.component.ts:34`).
  - La auditoría guarda `estadoRemoto` (`ServicioResolucionPoint.java:53`).

## Bajos (seguimiento, no impiden cerrar)
1. **Botón «Continuar cobro» sin efecto.** Aparece en órdenes `APROBADO` que no se pueden reembolsar (repartos o con `REVISAR:`), porque la plantilla solo excluye `CANCELADO/RECHAZADO/EXPIRADO/REEMBOLSADO` (`caja-sales-section.component.html:62`). Pero `continuarOrdenPoint` no hace nada con `APROBADO` (`.ts:170`). Conviene mostrar «Revisar» o nada cuando la orden está en `APROBADO`.
2. **Reembolso visto antes que la aprobación.** Si llega `REEMBOLSADO` desde `CREADO/EN_TERMINAL`, la venta sigue en `PENDIENTE_PAGO` y la orden pasa a un estado final:
   - no bloquea citas ni el corte;
   - el inventario queda descontado;
   - no hay acción en la UI, solo `REVISAR:`.

   Antes de M1 este caso se revertía. Ahora queda en revisión, que es lo seguro. Su resolución pertenece a 007-2.
3. **Terminal reasignada con una orden en curso.** `guardarTerminal` permite cambiar la sucursal de una terminal con una orden activa (`ServicioMercadoPagoPoint.java:113-120`). La orden aparecería en la Actividad de la sucursal nueva, mientras el 409 bloquea el corte de la sesión original. Conviene rechazar la reasignación si `existeEnCurso`.

## Límites
- No ejecuté suites, `git diff --check` ni la revisión visual. Codex informa que las suites completas se están repitiendo después de las pruebas nuevas, así que el cierre depende de ese resultado: backend con MySQL sin omitir y Angular.
- No revisé el arreglo del choque `legend` de Bootstrap ni el componente compartido, ni la copia de la UI con Point simulado.
- Se mantienen los límites anteriores: el corte cuenta el Point reembolsado de pagos divididos hasta 007-2, y quedan pendientes la validación de V139 y `ddl-auto: validate` en `respuestas/010.md`.

## Para cerrar 010
1. Resolver V1: comparar la referencia con cualquiera de los dos ids y agregar la prueba MySQL del camino válido.
2. Registrar en `respuestas/010.md` los resultados de las suites completas repetidas y de `git diff --check`.
3. Registrar los bajos 1–3 como seguimiento.
