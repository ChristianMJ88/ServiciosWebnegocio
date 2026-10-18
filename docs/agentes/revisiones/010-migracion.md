# 010-migración · Versión V139 y diseño de partes previstas Point

Autor: Claude Code (arquitectura y revisión, solo lectura), 2026-10-17.
Alcance: solicitud `010-migracion.md`, el esquema de `orden_point_caja` y `pago_venta`, y el flujo Point de los puntos 1–3 (`ServicioMercadoPagoPoint`, `ClienteMercadoPagoPoint`, `ServicioVentasCaja.confirmarPoint/cancelarPoint`).
Límites: no consulté ramas remotas porque no tengo git en esta sesión; para eso me baso en el fetch de Codex. No ejecuté pruebas.

## Veredicto

- **V139: no encontré colisión.** Hay una sola ubicación de migraciones (`application.yml:22`, `classpath:db/migration`), la más alta es `V138__folios_citas_existentes_con_guardas.sql` y no existe ningún `V139*` en el repositorio. Puede usarse cuando la reserva en ESTADO.md esté registrada (ya lo está, en la línea 3).
- **No hay otra estructura existente que sirva.** El diseño propuesto, con JSON en la orden y recibido/cambio en `pago_venta`, es correcto. Lo apruebo con los ajustes D1–D6.
- **Hay un bloqueante en el punto 1 (B1) que debe corregirse antes del 4.** Sin esa corrección, un pago dividido puede acabar en «tarjeta cobrada y efectivo devuelto».

---

## Alternativas existentes descartadas (con evidencia)

| Opción | Por qué no sirve |
|---|---|
| `orden_point_caja.detalle_estado` (V102:33) | Además de ser texto de 180, `aplicarEstado` lo **sobrescribe** en cada actualización del proveedor (`ServicioMercadoPagoPoint.java:233`). Lo previsto se perdería con el primer webhook. |
| `venta.observaciones` | `confirmarPoint` le agrega avisos de revisión (`ServicioVentasCaja.java:266`). Es texto libre que se ve en pantalla. |
| Guardarlo por venta en vez de por orden | Una venta Staff vuelve a `POR_COBRAR` cuando se cancela (`ServicioVentasCaja.java:292`) y se puede reenviar con `ventaPendienteId` (`:146`). Una venta puede tener varias órdenes Point a lo largo del tiempo, y lo previsto de un intento no debe pasar al siguiente. **La orden es el lugar correcto.** |
| `conciliacion_pago` (V105) | Guarda un registro por pago del proveedor, creado al recibir la respuesta remota. No tiene estructura para las partes en efectivo o manuales y mezclaría la conciliación con la caja. |
| `pago_venta` antes de cobrar | Descartado con razón: el corte y el saldo lo contarían como dinero recibido. |

No hay precedente de recibido/cambio: no aparece `recibido` ni `montoRecibido` en ningún archivo del backend, así que hoy el comprobante no se puede reimprimir con el cambio. Las columnas nuevas son necesarias si el comprobante (4.5) debe mostrarlo.

---

## Ajustes al diseño de V139

**D1 · Alta · Contenido inmutable y versionado.**
- `pagos_previstos_json JSON NULL`. Se escribe solo en T1 y no se modifica después: en la entidad, `@Column(name="pagos_previstos_json", columnDefinition="json", updatable=false)`, igual que en `CitaEntidad.java:72`.
- Formato propuesto: `{"v":1,"partes":[{"metodoPago":"EFECTIVO","monto":"300.00","montoRecibido":"500.00","referencia":null}]}`.
- Los importes van como cadenas con escala 2, nunca como números de punto flotante. Como máximo 2 partes que no sean Point.
- Sin notas ni datos personales. La `referencia` se limita a 120 caracteres, igual que `pago_venta.referencia` (V85:61).
- Al leer se deserializa a un DTO y se valida. Un JSON que no cumpla el formato produce un error interno con log, nunca un pago parcial.

**D2 · Alta · `confirmarPoint` debe usar el importe de la orden.**
- Hoy `pago.setMonto(venta.getTotal())` (`ServicioVentasCaja.java:272`). Con pago dividido, eso registraría 700 en Point y además los 300 previstos.
- Cambiar la firma para que reciba la orden (o su `importe` y lo previsto) desde `aplicarEstado:235`.
- En la misma transacción se comprueba `orden.importe + Σprevistas == venta.total`. Si no cuadra, **no se rechaza**, porque la tarjeta ya se cobró: se registra solo el pago Point y se marca para revisión con el mismo mecanismo que 1.4 (`:264-268`).
- La idempotencia actual, que detecta si ya existe el pago Point (`:255-257`), sigue protegiendo también las partes previstas, porque todo ocurre en una sola transacción.

**D3 · Media · El efectivo previsto no debe alterar un corte cerrado.**
- El efectivo se registra en `venta.cajaSesionId` al confirmar. Si la sesión se cerró entre T1 y la aprobación, el corte ya cerrado cambia.
- Recomiendo que **el cierre de caja responda 409** mientras haya una orden Point no final con partes previstas en EFECTIVO de esa sesión: «Hay un cobro Point en curso con efectivo pendiente de registrar». Es más simple y seguro que reasignar a otra sesión.
- No hace falta otra migración: basta una consulta sobre `orden_point_caja` unida a `venta.caja_sesion_id`.

**D4 · Media · Columnas de `pago_venta`.**
- `monto_recibido DECIMAL(12,2) NULL` y `cambio DECIMAL(12,2) NULL`, y nada más.
- Validación en el backend: solo cuando el método es EFECTIVO, `monto_recibido >= monto` y `cambio = monto_recibido − monto`.
- No añadir `CHECK` en la base: el resto del esquema no los usa y los registros históricos los tienen en NULL.

**D5 · Baja · DDL no transaccional en MySQL.**
- V139 tiene dos `ALTER TABLE`. Si falla el segundo, el primero queda aplicado y Flyway marca la migración como fallida.
- Es aceptable porque son cambios que solo agregan columnas. Hay que probarla en MySQL efímero igual que `MigracionesPoliticaMysqlTest`: aplicarla sobre V138 y comprobar las columnas, que sean NULL y que no cambie ningún registro anterior. La otra opción es separarla en V139 y V140, reservando las dos.
- Sin `UPDATE` y sin backfill, como propone Codex.

**D6 · Baja · `ddl-auto: update`** (`application.yml:14`). La migración y el campo de la entidad deben ir en el mismo cambio, con `columnDefinition` explícito, para que Hibernate nunca cree la columna con otro tipo en un entorno donde Flyway no corrió primero.

---

## Bloqueante del punto 1 (implementado)

**B1 · Alta · `enviarCreacion` cancela la venta ante errores que no son definitivos.**

Evidencia:
- `ServicioMercadoPagoPoint.java:158-166` captura cualquier `RuntimeException` que no sea `ResourceAccessException`, cancela la orden local y la venta, y libera el saldo.
- `ClienteMercadoPagoPoint.java:124` convierte **todo** error HTTP, también los 5xx, en `IllegalStateException`. `:111` (cuerpo vacío) y `:127` (falta el `id`) lanzan esa misma excepción.

Por qué es grave:
- Un 502/503/504, una respuesta vacía o una respuesta sin `id` pueden llegar **después** de que Mercado Pago creó la orden. La terminal la muestra durante 16 minutos (`:57`, `PT16M`).
- Si el cliente paga, el webhook busca la orden por `proveedorOrdenId` (`:221`), que quedó en NULL. Devuelve `false` y el cobro queda huérfano, con la venta CANCELADA y el saldo libre para cobrarse otra vez.
- Lo mismo pasa con un 409 al **reenviar** desde `consultar` (`:186`): el «cobro pendiente en la terminal» puede ser nuestra propia orden.
- Con el punto 4, la UI además le diría al cajero «devuelve el efectivo» aunque la tarjeta sí se cobró.

Corrección:
1. El cliente lanza una excepción con tipo y código HTTP, por ejemplo `ErrorProveedorPoint(status)`, en lugar de comparar el texto `"HTTP 409"` (`:167`).
2. Solo se cancela en el **primer envío** y solo ante un 4xx definitivo (400, 401, 403, 404, 422, y 409 «terminal ocupada»).
3. Un 5xx, una respuesta vacía, una respuesta sin `id`, o cualquier error en un **reenvío**, se tratan igual que el timeout: la orden queda en `CREANDO`, el saldo sigue apartado y el siguiente `consultar` reenvía con la misma idempotencia.

Pruebas:
- 503 en el primer envío: orden `CREANDO`, venta `PENDIENTE_PAGO` y un pago directo de la misma cita recibe 409.
- Respuesta sin `id`: el mismo resultado.
- 409 en un reenvío: no se cancela.
- 400 en el primer envío: se cancela y el saldo se libera (la regresión actual).

## Seguimientos del 1–3 (no bloquean V139)

**S1 · Media · `CREANDO` sin salida acotada.**
- `cancelar` exige `proveedorOrdenId` (`:196`), así que una orden que queda indeterminada solo se puede consultar.
- Si se consulta horas después, el reenvío puede caer fuera de la ventana de idempotencia del proveedor y **crear una orden nueva** en la terminal.
- Propuesta sin migración: si `CREANDO` dura más que `expiration_time` (16 minutos) más un margen, `consultar` **no reenvía**. Pasa a un estado `REQUIERE_REVISION` (cabe en `estado VARCHAR(30)`), mantiene el saldo apartado y muestra «Verifica en Mercado Pago si se cobró antes de liberar».
- Hay que incluir ese estado en `existeEnCurso` (`OrdenPointCajaRepositorio.java:13`) y en la regla de saldo apartado.

**S2 · Baja · Orden de bloqueo.**
- T1 bloquea la terminal (`:122`) antes que todo lo demás. El orden real queda así: **terminal → existencias → prestadores → citas**.
- Hay que documentarlo junto a `bloquearGrupoOrden` y comprobar que ninguna ruta bloquee la terminal después de haber bloqueado citas o existencias.

## Pruebas adicionales para V139 y el punto 4

- MySQL efímero: V139 se aplica sobre V138, las columnas quedan en NULL y ningún registro anterior cambia de huella (el mismo enfoque que en V138).
- Pago dividido con Point aprobado: se crean 2 `pago_venta` (Point = `orden.importe` y EFECTIVO con recibido y cambio) y un movimiento de 300, en una sola transacción.
- Orden rechazada o cancelada: 0 pagos y 0 movimientos. Lo previsto queda solo en esa orden; un reenvío de la misma venta Staff empieza sin partes previstas.
- Intento de modificar `pagos_previstos_json` después de T1: no se persiste.
- Cierre de caja con una orden Point en curso y efectivo previsto: 409 (D3).
