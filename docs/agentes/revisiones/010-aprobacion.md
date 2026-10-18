# 010: aprobación final

Autor: Claude Code, 2026-10-18. Revisión de solo lectura: conté los XML y los logs y leí el código. No ejecuté las suites.

**Resultado: 010 aprobada.** El commit queda pendiente de que el usuario lo autorice. Se cumplen las tres condiciones de `010-verificacion-final.md`.

## Condiciones
1. **V1 resuelto.**
   - `marcarReembolsadaPoint` (`ServicioVentasCaja.java:336-339`) acepta la referencia por `proveedorPagoId` **o** por `proveedorOrdenId`, los dos de la misma orden. Un reintento sobre una venta ya `REEMBOLSADA` devuelve `true` sin volver a reintegrar.
   - Hay una prueba MySQL del camino válido (`CobrosConcurrentesMysqlTest.java:323-324`): devuelve `true`, la venta queda `REEMBOLSADA`, el saldo vuelve a 700 y `reintegrarVenta` se llama **una sola vez** aunque se repita.
   - Codex reporta también la variante con y sin `pagoId` inicial.
2. **Suites y diff, contados por mí.**
   - **Backend:** los XML suman 151 suites y 576 pruebas, con 0 fallos, 0 errores y 1 omitida (correo real). Se generaron entre las 05:40 y las 05:42, después del último cambio del backend (05:38:37).
   - **Angular:** `/private/tmp/fluora-010-angular.log` (06:40:40) da 65 archivos y 193 pruebas aprobadas, después del último cambio del frontend (06:37:09).
   - `git diff --check`: limpio.
3. **Bajos.**
   - Bajo 1 está **corregido**: en una orden `APROBADO` que no se puede reembolsar, el botón ahora dice «Revisar incidencia» y ya no aparece un «Continuar cobro» que no hacía nada (`caja-sales-section.component.html:62`).
   - Bajo 2 (un reembolso que llega antes de la aprobación) pasa a 007-2.
   - Bajo 3 (reasignar una terminal que tiene una orden en curso) **sigue abierto**: `guardarTerminal` (`ServicioMercadoPagoPoint.java:113-120`) no consulta `existeEnCurso`. Está registrado como seguimiento.

## Observaciones nuevas

### O-H1 · Media (antes de 011): las pruebas pueden escribir en la base habitual
- El incidente de V139 (`respuestas/010.md` §«Incidencia del harness») borró **tres columnas de la base local habitual**. Se restauraron con el mismo DDL. Las huellas no cubren los valores previos de esas columnas.
- La causa de fondo sigue presente: las pruebas MySQL usan **el usuario administrador local** para crear y borrar los esquemas efímeros. Ese usuario también puede escribir en `agenda_db`.
- 011 va a agregar más pruebas que escriben (Postman contra una copia). **Antes de empezar 011:**
  - crear un usuario MySQL local solo para pruebas, con privilegios **únicamente** sobre un patrón de esquemas efímeros (por ejemplo, `fluora_test_%`), sin acceso a `agenda_db`;
  - usarlo en las pruebas MySQL de Java y en la copia de 011;
  - agregar una guarda que aborte si `DATABASE()` no coincide con ese patrón.
- Las credenciales van en variables de entorno, nunca en el repo.

### O-H2 · Informativa: `ddl-auto` por defecto ahora es `validate`
- `application.yml:14` pasa de `update` a `${JPA_DDL_AUTO:validate}`. En `application-prod.yml:20` ya era `validate`, así que producción no cambia.
- Los entornos locales que dependían de que Hibernate creara columnas ahora fallan al arrancar si falta una migración. Es lo correcto con Flyway. Hay que mencionarlo en el README o en las notas del commit.

## Postman CLI (preparación de 011)
- Versión 1.75.0, con sesión iniciada. Comprobé una colección **local** de humo (`GET /actuator/health` contra 8080, solo lectura): 1 petición, 1 aserción, 0 fallos.
- Las colecciones locales **no publican** los resultados en la nube de Postman. Solo se publican cuando se corre por id de colección o desde una API enlazada a git. Para 011: **correr siempre desde el archivo del repo, no por id**, para que no salgan datos de prueba ni respuestas a la nube sin una decisión del usuario.

## Adenda 2026-10-18: commit autorizado
El usuario **autorizó el commit de 010**. Codex lo hace antes de O-H1, 011 y 008, con las mismas reglas de 85d3cc2:
- solo los archivos de 010, más su respuesta y sus revisiones;
- se conservan fuera las revisiones ajenas;
- el mensaje menciona el cambio de `ddl-auto` (O-H2) y la migración V139;
- sin push ni despliegue.

Orden siguiente: **commit 010 → O-H1 → 011 (línea base) → 008 (F1–F4)**. Ver `011-pruebas-api.md` y `008-decisiones.md`.

## Límites
- Sin despliegue: siguen pendientes 007-2/3, A1 de 004 y `binlog_format` en el destino.
- La terminal física y la moneda real del sandbox siguen sin certificar.
- No revisé de nuevo el componente compartido completo: lo revisaron `010-implementacion` y `010-cierre`. Esta revisión cubre lo que cambió después de `010-verificacion-final`.
