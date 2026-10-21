**Veredicto:** apruebo el cierre local de R3, con una condición documental (P2) que hay que resolver antes de registrar el cierre en ESTADO. No hay hallazgos P0 ni P1. No autorizo commit, push, deploy, prerender productivo ni cambios en Hosting o Functions.

**Qué revisé:** solo lectura de PROTOCOLO, ESTADO, la respuesta `008-f4-m3-r3.md` (estado vigente y sección final), las revisiones `r3-cta-residual` y `r3-residual-codigo`, el código de `nav-cta-geometry.mjs`, `layout-residual-criteria.mjs`, `layout-residual-probe.mjs`, `nav-cta-probe.mjs`, `browser-probe.mjs` y `probe-page.mjs` con sus tests, la plantilla del nav y las tres evidencias portables. No ejecuté nada. No leí los logs ni los JSON de `/private/tmp`, ni las capturas: las cifras de esos archivos (Node 90, Angular 367, UI 4200, procesos y perfiles) las tomo de tu declaración.

## Puntos que pediste verificar

**1. Signo de la fórmula: tu corrección es correcta y mi condición 4 tenía el signo invertido.**
- En `marketing-nav.component.html:11` el texto va antes del icono dentro de un grupo centrado. Con la caja fija de ancho B, la posición del icono es `x = B/2 + (t + gap − i)/2`. Por lo tanto `dx = (t_después − t_antes)/2`.
- `nav-cta-geometry.mjs:8` usa esa fórmula.
- El caso de `layout-residual-criteria.test.mjs:20-21` (texto de 50 a 38 px, icono de 70 a 64) solo pasa con el signo correcto. Con mi fórmula esperaría +6 frente a un dx de −6 y daría structural, así que el test ya protege contra la fórmula inversa.
- La evidencia coincide: `layout-verified.json:338-339` registra dx −6.125 frente a −6.1328125 esperado.

**2. Fase de fuente: tu lectura es correcta. «0 en fuente» de mi condición 5 fue un error mío, y queda reemplazada por la tabla P1-c.** Así se cumplen sus límites:
- La reclasificación solo se aplica a `groups.hydration` (`layout-residual-probe.mjs:35`). Los desplazamientos de la fase de fuente no se reclasifican ni se restan, y siguen dentro de `final.cls ≤ .01` (`:38`). El límite global no se relaja.
- **Límite que queda declarado:** en la fase de fuente, el nav solo está acotado por el presupuesto global de CLS, no por geometría. Mi preocupación de los 71 px (que la caja crezca con la fuente de respaldo) queda cubierta por los fixtures con fuentes retenidas. Ahí `before` y `after` se miden con la fuente de respaldo y se exige `sameBox` (`nav-cta-probe.mjs:10-15`). `afterFonts` se guarda solo como información. El desplazamiento del nav de unos 4 px durante el cambio de fuente sigue siendo un residual reportado.

**3. Cleanup: correcto.** `closeProcessGroup` (`browser-probe.mjs:90-98`) consulta ps antes de cualquier señal. Un grupo vacío se da por cerrado sin señalizar. Un proceso ajeno o que no es Chrome produce error. EPERM nunca se captura. Si llega ESRCH pero ps todavía ve miembros, también es error. Los tests de `browser-probe.test.mjs:48-57` cubren todos esos casos.
- Queda un riesgo mínimo, que acepto sin cambios: entre la lectura de ps y el `kill` hay una pequeña ventana de tiempo. Además, la expresión `/Chrome/` reconocería tu Chrome habitual solo si reutilizara exactamente el PID líder del grupo.

**4. Fixture inválido: correcto.** `probe-page.mjs:9` lo rechaza antes de leer el artefacto o abrir Chrome, y la unidad está en `layout-residual-criteria.test.mjs:28`.

**Evidencia portable:**
- `normal-verified.json`: SHA 42d6b0a8, 32 muestras, máximo 0.00666254, sin violaciones.
- `layout-verified.json`: SHA 42d6b0a8, 52 muestras, el ensamblaje declarado en `:27085-27089` con `sourceBuildsEqual:true`, y un aporte del CTA entre 1.47e-6 y 2.11e-6.

## Hallazgos

**P2. La evidencia del CTA no está atada a ningún build.**
- `nav-cta-probe.mjs` no llama a `fingerprintPublicBuild`, a diferencia de `layout-residual-probe.mjs:15`. Por eso `cta-verified.json:1-5` no tiene campo `build`.
- Atribuir los 12 fixtures a 42d6b0a8 es hoy solo una afirmación.
- **Corrección (elegir una):**
  - (a) Añadir `build` al JSON de salida en `:18` y repetir solo los 12 fixtures.
  - (b) Declararlo en la respuesta como límite: «12 fixtures sin huella; se infiere 42d6b0a8 porque el root no cambió».
- **Validación:** que `cta-verified.json` tenga `build` igual a 42d6b0a8, o que el límite quede escrito en la respuesta y en ESTADO.

**P3, no bloqueantes:**
- **Faltan dos casos negativos** en `layout-residual-criteria.test.mjs:19-25`: misma etiqueta con `knownNavCta`, y `dy ≠ 0` o tamaño distinto. Los dos deberían dar structural. El código ya los exige (`nav-cta-geometry.mjs:9,12`).
- **Nombre engañoso** en `nav-cta-probe.mjs:10`: `fallback` y `before` se evalúan uno tras otro y son la misma medición. Sin fuentes retenidas, `fallback` contiene en realidad geometría con Manrope. `beforeFontState` lo aclara, así que basta con anotarlo en el README o eliminar el campo.

## Qué sigue

Una vez resuelto P2 por cualquiera de las dos vías, R3 queda cerrada como probe local. Se mantienen tus límites: producción sigue en CSR, F4 sigue abierta, no hay prueba en Safari o iOS físico ni en host propio, no se certifica el 85 de Lighthouse, y quedan los residuales de scroll, loading y cambio de fuente. El siguiente paso productivo necesita la revisión del usuario.

No tengo herramienta de escritura. Codex puede guardar esta revisión como `revisiones/008-f4-m3-r3-cierre-final.md`.
