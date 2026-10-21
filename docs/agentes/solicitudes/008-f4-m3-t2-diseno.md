# T2: inestabilidad de montaje DOM, antes de R3

Autor Codex, 2026-10-20. Usuario aprueba checkpointR4 (ya e53cc51), corregir prueba inestable y continuar R3. Solo lectura, no ejecutar suites/builds ni escribir código. No leer .env/postman/credenciales. Diseña corrección acotada de tests; ningún cambio de lógica aplicación, workers o timeouts ni quitar aserciones.

Leída revisión008-f4-m3-r4-verificacion. Reproducido inmediatamente con suite normal Node20.19/TZAmerica/Mexico_City, sin carga pesada propia simultánea:338/341,82/85archivos,43.79s; log `/private/tmp/fluora-m3-t2-reproduce-1.log`. Mensaje exacto en tres casos: **Error: Test timed out in 5000ms.**
- booking-shell.spec.ts:19, error:false,6234ms. Error:true1967ms, tercer caso1350ms.
- site-promotions.component.spec.ts:8 sends existing promotion ID,5382ms.
- caja-payments-section.component.spec.ts:50 presenta importes backend,5324ms.
No falló una aserción de estado. Primeros montajes de componentes pesados + compileComponents dentro de it asíncrono consumen presupuesto con paralelismo normal; la segunda instancia en el archivo es más rápida. No hay evidencia de que un sleep/whenStable por sí solo resuelva coste de compilación; no inventar carrera de precios.

Lee esos tres specs y componentes/hijos solo cuanto haga falta. BookingShell debe conservar plantilla real, DOM de formulario idéntico, inert, estado fuera y contenido monetario neutro/reintento; calendario diferido ya manual. Caja conserva DOM real importes backend y comprobante/contexto; Promociones conserva payload ID y expansión/calendario. Puede separar tests de clase/contrato de las integraciones DOM.

Evalúa: preparación compartida por archivo en beforeEach (configure+compile+fixture inicial donde corresponda) para separar montaje de las aserciones, manteniendo teardown por caso y límites predeterminados, y/o stubs explícitos de hijos no ejercidos (evitar reemplazar template bajo prueba). No usar NO_ERRORS_SCHEMA, saltarse aserciones ni esconder la lentitud cambiando timeout/worker. Si mover setup a hooks solo oculta el problema, indícalo y propone alternativa concreta.

Para estados de Angular, esperar fixture.whenStable tras cambios emitidos antes de afirmar DOM, sin timer real. Agregar controles de suscripción antes de emitir Subjects si necesario. Criterio5suites normales consecutivas verdes, registrar conteos/duración y cualquier caso>4s; fallo reinicia serie, nada de desactivar casos. No mezclar R3 antes de T2cerrado.

Devuelve diseño concreto y justificación; el script guarda revisiones/008-f4-m3-t2-diseno.md.
