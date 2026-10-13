# Estado de coordinación

- Claude Code instalado y disponible en la terminal.
- Roles: Claude arquitectura/revisión; Codex implementación/verificación.
- Tarea 001: revisión de promociones en reservas, checkout y Caja.
- Revisión de Claude recibida en revisiones/001.md y cierre en revisiones/001-cierre.md: aprobado sin bloqueantes. Correcciones iniciales en respuestas/001.md, incluidas decisiones A2/A3 aprobadas por el usuario.
- C1/C2 del cierre corregidos y verificados por Codex: snapshot ilegible omite título con warning; ventas sin cliente no consultan ClienteRepositorio. Resultado en respuestas/001-cierre.md: 6 pruebas backend aprobadas, cero fallos/errores; git diff --check sin errores. C3/C4 permanecen como observaciones futuras.
- Verificación anterior completa (antes de C1/C2): backend 293 aprobadas y 1 omitida; frontend 15 aprobadas. Para C1/C2 se ejecutaron las dos suites afectadas; no se repitió la suite completa ni frontend.
- Caja revisada en navegador con sesión real: no hay citas cobrables del día. Pendiente comprobar visualmente el desglose de una cita promocional real; sus importes y render se verificaron con pruebas backend y componente.
- Existe trabajo anterior sin commit en Caja, Recepción, Booking, Home y otros archivos. Conservarlo.
- Detalle de implementación y regla de anticipo: `docs/promociones-reservas.md`.
- Suite completa posterior a C1/C2 (ejecutada por Claude Code a petición del usuario, 2026-10-12, sin commit):
  - Backend `mvn test`: 296 pruebas, 295 aprobadas, 1 omitida, 0 fallos, 0 errores (exit 0).
  - Angular `ng test --watch=false` (Vitest): 148 pruebas en 55 archivos; 147 aprobadas y **1 fallida**. Todas las pruebas de 001 pasan (Booking, promociones del sitio, cobros de Caja).
  - Falla ajena a 001: `src/app/pages/admin-dashboard/providers/admin-providers.facade.spec.ts` › "elige crear o actualizar según exista un id" → `TypeError: this.adminService.invitarPrestador is not a function`. El facade (commit 8ebdb06) llama a `invitarPrestador` cuando el payload no trae `nombreUsuario`, pero el mock del spec solo define `crearPrestador` y `actualizarPrestador`. Facade y spec no tienen cambios en el working tree; la falla viene del código ya commiteado. Pendiente: decidir si el fixture debe incluir `nombreUsuario` o si el mock debe definir `invitarPrestador`; es tarea aparte de 001.
- V130/V131 verificadas contra todas las ramas locales y referencias de origin tras `git fetch --all` (2026-10-12): sin colisiones ni versiones duplicadas. Solo existen sin commit en el working tree actual; la rama actual y su remoto llegan a V129. Evidencia y límites en respuestas/001-migraciones.md. Revalidar antes de integrar si se publican cambios nuevos.
