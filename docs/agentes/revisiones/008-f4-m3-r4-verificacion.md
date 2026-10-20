# 008 F4 M3 · R4: verificación ejecutada por Claude

Autor: Claude Code, 2026-10-20. La revisión de cierre (`008-f4-m3-r4-cierre.md`) fue solo de lectura. **Esta vez ejecuté yo las verificaciones**, ya con el código posterior al último ajuste (`platform-host.service.ts`, 05:08). No cambié código ni desplegué nada.

## Resultados
- **Suite Angular con la configuración normal** (Node 20.19, `TZ=America/Mexico_City`, sin worker temporal): **5 corridas completas. 4 en verde (85/85 archivos, 341/341) y 1 con un fallo (340/341).**
  - El fallo: `booking-shell.spec.ts` › «mantiene la precarga inert y conserva el formulario hasta resolver la selección (error: false)».
  - Corrido solo, 3 veces seguidas: 3/3 en verde.
  - Ese spec viene de `15960c9` (CLS de booking). **No es de R4:** R4 no lo toca.
- **Builds:**
  - marketing: 477.19 / 130.51 kB, con el shell CSR validado;
  - application: 826.09 / 168.16 kB;
  - `check-public-builds` en verde;
  - `index.csr.html` idéntico byte a byte a `index.html`.
- **Node 22.21.1:** `functions` 25/25; preview y `prepare-csr-shell` 12/12.
- `git diff --check`: limpio.

## Dictamen
- **R4 aprobada.** Las cifras de Codex coinciden con las mías. El aumento de +1.02 kB en marketing queda documentado y es aceptable. No apruebo nada sobre Lighthouse ni sobre la meta de 85 porque no hubo medición nueva: eso corresponde a R3 y al prerender.
- **Nuevo, T2 · Media (no bloquea R4; se corrige antes del commit de R3):** `booking-shell.spec.ts` es **intermitente con carga en paralelo** (1 de cada 5 corridas). Es el mismo tipo de deuda que T1.

**Corrección (Codex):**
1. Reproducir el fallo con la suite completa y registrar el mensaje exacto de la aserción.
2. Buscar la causa: suele ser una espera que depende del tiempo (timers reales, `setTimeout`, RAF o `afterRenderEffect`) en lugar de una condición observable.
3. Esperar el estado (`whenStable` o `fixture.whenRenderingDone`, o timers falsos controlados), **sin subir timeouts ni debilitar las aserciones**.
4. Criterio: `npm test` normal en verde **5 veces seguidas**, con el conteo de cada corrida.

## Siguiente paso
- Se puede autorizar **R3**: hidratación, `provideClientHydration` y placeholders con medidas reales, según `008-f4-m3-diseno.md` §3.2-R3.
- Antes, o como primer paso de R3, conviene un **commit de R4** como punto de control, sin push ni despliegue. Lo autoriza el usuario.
- R3 es donde por fin corre el código en el servidor y se hidrata. Es el paso de más riesgo de M3, así que se revisa por partes, igual que R1 y R4.
