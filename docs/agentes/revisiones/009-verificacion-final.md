# 009 y 007-1 (C1–C4): verificación final

Autor: Claude Code, 2026-10-17. Revisión de solo lectura: conté los XML y los logs y leí el código. No ejecuté pruebas.

**Resultado:** se cumplen las cinco condiciones de `009-cierre.md`. **009 P1–P3 y C1–C4 de 007-1 quedan aprobadas.** El commit queda pendiente de que el usuario lo autorice.

## Condiciones

1. **Backend**
   - Conté los XML: 143 suites, 543 pruebas, 0 fallos, 0 errores y 1 omitida (envío real).
   - Los XML son de las 12:02–12:03, posteriores al último cambio del backend (12:01:15: `ServicioSaldoCitas` MANDATORY, `CobrosConcurrentesMysqlTest`, `ServicioPoliticaReservaPagoTest`).
   - `CobrosConcurrentesMysqlTest`: 8 pruebas, 0 omitidas. `MigracionesPoliticaMysqlTest`: 1 prueba, 0 omitidas.
2. **Angular y diff**
   - `/private/tmp/fluora-009-angular.log` (12:10:49): 64 archivos y 182 pruebas aprobadas.
   - La ejecución es posterior al último cambio del frontend (12:09:55, spec de Recepción).
   - `git diff --check` sin errores.
3. **Revisión visual**
   - Recepción, POS y Cobros revisados en 375×812, 820×1180 y 1440×900, sin desbordamiento, con datos existentes y sin cobrar. Las capturas están en `/private/tmp/fluora-009-*`.
   - «Atendida · por cerrar» con SIN_COSTO solo se comprobó en el componente. Lo acepto.
4. **Disposición de hallazgos**
   - B6 está resuelto: se comprueba también la segunda cita y `calcularBajoBloqueo` usa `MANDATORY` (`ServicioSaldoCitas.java:43-46`).
   - B4, B5/O2 y O1 quedan registrados como seguimiento en respuestas/009.md.
5. **`binlog_format`**
   - En local es ROW. Hay que comprobarlo en el destino antes de desplegar.

## Comprobaciones puntuales en el código

- **P1** (`ServicioPoliticaReservaPago.java:31-35`): en una reserva pública con importe mayor que 0 y sin cobro en línea, lanza `AnticipoNoDisponibleException`. Ya no pasa sin aviso a pago en el negocio. Cuando `publico=false`, el comportamiento no cambia.
- **P2** (`SeccionAgendaCita.java`): la regla coincide con la decisión del usuario y se aplica en un solo lugar.
- **P3** (`ServicioVentasCaja.java:180-200`):
  - bloquea cada cita en orden ascendente;
  - valida sucursal, estado de cobro, cliente y moneda antes de calcular el saldo;
  - calcula el saldo bajo el bloqueo y rechaza si alguna cita ya está pagada, antes de escribir;
  - Point con varias citas se rechaza antes de llamar al proveedor (`:145`).

## Pendientes sin cambios

- Seguimientos: B4 (webhook en RR), B5/O2 (timeouts y bloqueo de Point, Point con varias citas) y O1 (anticipo en el bot).
- Queda 1 cita local con anticipo, en PENDIENTE y sin pago. Se conservó así; el usuario decide qué hacer con ella.
- La política v1 de Nail Art no la publicó Codex. El usuario debe confirmar que la publicó él y que su texto es el que quiere.
- Despliegue: sigue bloqueado hasta 007-2/3 y A1 de 004, además de comprobar `binlog_format` en el destino.
- Nota de forma: `009-cierre.md` empieza con un preámbulo de la sesión anterior («No tengo herramienta de escritura…»). Su contenido sigue siendo válido.
