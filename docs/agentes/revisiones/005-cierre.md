No pude registrar el veredicto en `docs/agentes/revisiones/005.md` porque en esta sesión solo tengo herramientas de lectura. Abajo va el texto listo para pegar. No ejecuté pruebas ni comandos, así que no repetí la `ReporteCobroCitasTest` ni las suites. Me baso en el código y en que Codex reporta exit 0.

---

## Revisión del paso 5 (reportes) y T6-c, 2026-10-16

Autor: Claude Code (solo lectura). Revisé: `ServicioReporteCobroCitas`, `ReporteCobroCitasRepositorio`, `FinanzasCitasResponse`, la integración en `ServicioAdminCitas.resumen` (`ServicioAdminCitas.java:393-411`), el overview `admin-summary-section.component.html:68-86` con su spec, `ReporteCobroCitasTest` y `ServicioCancelacionCita.motivoRechazo`. Este veredicto cubre solo este bloque. No cambia lo que ya estaba aprobado de la 005.

### Veredicto
**Paso 5 y T6-c aprobados, con R1 a corregir antes del commit.** R2–R4 son de seguimiento.

**Lo que está bien:**
- **Tenant y sucursal.** Cada rama filtra por empresa. Los joins usan `empresa_id` en ambos lados: pago_cita↔cita, venta↔detalle y detalle↔cita. La subconsulta de pago_venta también filtra por empresa. Las sucursales fuera de alcance se saltan antes de pedir su zona (la prueba lo comprueba con `verify(zonas,never()).de(7L,4L)`). El pago inconsistente de la empresa 8 sobre una cita de la 7 queda fuera.
- **Periodo de citas.** Programado y devengado usan `c.inicio` local con `[desde, hasta+1)`.
- **Periodo de pagos.** Los cobros se filtran por `registrado_en` UTC, convertido con la zona de cada sucursal (`ServicioReporteCobroCitas.java:33-35`). La prueba cubre las fronteras de medianoche: 05:59:59Z queda fuera del inicio y dentro del fin.
- **Deuda sin ventana (T5).** La rama de pendiente no recibe el periodo. Su predicado coincide con `CitaRepositorio.COBRABLE_CON_SALDO` (`CitaRepositorio.java:15-22`): mismos estados, check-in o FINALIZADA, pago_cita más venta_detalle SERVICIO de venta PAGADA. `greatest(...,0)` evita deuda negativa por sobrepago.
- **Ventas mixtas.** Cada pago de una venta mixta se reparte entre sus detalles de servicio, y los productos quedan fuera (`tipo_concepto='SERVICIO'`). Los pagos se agrupan por venta antes del join, así que un detalle no se repite por cada pago. Usar `pagos.total` como denominador garantiza que la suma de todos los periodos dé exactamente `d.total`. Con la proyección online (`ServicioVentasCheckout.java:55-84`) no hay doble conteo: el anticipo entra como venta ONLINE y el resto como pago_cita.
- **Moneda y redondeo.** Los importes se agrupan por moneda y nunca se mezclan. Se suma en BigDecimal entre sucursales y se redondea una sola vez, HALF_UP a 2 decimales.
- **Custodia.** Va como `null/null/false` y la UI muestra "aún no está disponible". No se supone que la 007 existe.
- **UI.** Muestra solo `finanzasCitas` por moneda, con etiquetas separadas: devengado, cobrado y "Por cobrar · histórico". La spec comprueba que el valor legado (999) no aparece. El resumen conserva los campos anteriores para compatibilidad.
- **T6-c.** El nuevo texto está en `ServicioCancelacionCita.java:65`. No queda el mensaje anterior en el código y no se abrió ninguna ruta de cancelación con pagos.

### Hallazgos

**R1 (media, antes del commit): RBAC de los importes financieros.**
`/resumen` solo exige `PANEL_ADMIN_ACCESO` (`ControladorAdminCitas.java:84`). Ahora devuelve lo cobrado y la deuda histórica. Las plantillas de supervisor y administrador operativo incluyen `REPORTES_ADMIN_VER` (`ServicioAdminCitas.java:1962,1977`), pero un rol personalizado puede tener panel sin ese permiso. Además, `ServicioReporteCobroCitas.java:26` lanza 404 si el usuario es de plataforma o no tiene empresa. Eso tumba el resumen entero, que antes respondía.
- **Corrección:** en `resumen`, devolver `finanzasCitas = List.of()` si el usuario no tiene `REPORTES_ADMIN_VER` o es de plataforma, sin lanzar excepción. Dejar la excepción solo para un uso directo del servicio.
- **Prueba esperada:** usuario con `PANEL_ADMIN_ACCESO` sin `REPORTES_ADMIN_VER` → 200, conteos intactos y `finanzasCitas` vacío, sin consultar el repositorio de reporte (`verify never`). La UI ya muestra el `@empty`.

**R2 (baja, seguimiento): reparto con céntimos periódicos.**
Al dividir un pago en periodos, cada periodo se redondea por separado y la suma puede diferir 0.01 del total. La prueba actual usa importes exactos (250/500).
- **Prueba sugerida:** servicio 100 en una venta de 300 con tres pagos de 100 en días distintos. Comprobar 33.33 por día y 100.00 en el histórico.

**R3 (baja, nota): los reembolsos cambian el cobrado de periodos pasados.**
Solo cuentan ventas `PAGADA`. Una venta que después pasa a REEMBOLSADA desaparece del cobrado de su periodo original. Los pagos de citas CANCELADAS sí cuentan como cobrado; la prueba lo exige (430 incluye 50). Me parece correcto como "dinero recibido", pero el tratamiento de anticipos retenidos o reembolsados corresponde a la 007, no a la 005.

**R4 (baja, preexistente): los campos legados usan otra frontera de fin.**
`citasReporte` usa `InicioBetween`, que incluye el fin (`ServicioAdminCitas.java:511`). `finanzasCitas` usa `<`, así que una cita justo a las 00:00 del día siguiente cuenta distinto en los campos legados. No afecta a la UI nueva. Corregirlo cuando se retiren esos campos.

**Rendimiento (nota):** la subconsulta de pago_venta recorre todos los pagos de la empresa en cada sucursal. Es aceptable con el volumen actual. Si crece, mover el filtro de sucursal dentro de la subconsulta.

### Fuera de este veredicto
- **Sesión real de Staff:** sin evidencia en esta revisión, **no aprobada**.
- **D2 y T6(a):** pasan a la 007. **S2:** pasa a la 007-2. No se amplió el alcance.
- **Cierre de la 005:** queda condicionado a R1, a la evidencia visual de Staff y a que las suites completas terminen en verde. Después, commit propio de la 005 según la autorización del usuario.
