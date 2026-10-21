# 008 F4 M3 · R3: verificación ejecutada por Claude

Autor: Claude Code, 2026-10-20. La revisión de cierre (`008-f4-m3-r3-cierre-final.md`) fue de solo lectura. Esta vez **ejecuté yo** las verificaciones sobre el estado actual del working tree, después de las correcciones P2/P3. No cambié código, no hice commit ni despliegue, y no leí `.env`, `postman/` ni credenciales.

## Correcciones de la revisión de cierre
- **P2 · Cerrado.** `scripts/performance/nav-cta-probe.mjs:5` calcula `fingerprintPublicBuild` y `:18,21` lo escriben en el resultado y en el JSON de fallo. `evidencias/008-f4-m3-r3-cta-verified.json` tiene `build` = `42d6b0a8…`, el mismo que `normal-verified` y `layout-verified`.
- **P3 · Cerrados.**
  - `layout-residual-criteria.test.mjs:25-27` agrega misma etiqueta, desplazamiento vertical y cambio de tamaño. Los tres esperan *structural*.
  - Se eliminó el campo `fallback` duplicado. `beforeFontState` (`nav-cta-probe.mjs:17`) indica la fuente de cada muestra.

## Resultados (Node 20.19 para Angular, Node 22 para scripts)
| Verificación | Resultado |
|---|---|
| `functions` (`node --test`) | 25/25 |
| `scripts/*.test.mjs` + `scripts/performance/*.test.mjs` | 65/65 (total 90, igual a Codex) |
| `npm run build` | exit 0. Marketing **503.12 / 138.51 kB**, «Marketing CSR shell validated». Application **826.72 / 168.32 kB** |
| `check:public-builds` | verde |
| `cmp index.csr.html index.html` | idénticos: producción sigue en CSR |
| `index.html` de producción | `<app-root>` vacío, sin `ngh`/estado de hidratación, sin código de `platform-server` en los JS |
| `npm test` normal (`TZ=America/Mexico_City`) | **3 corridas seguidas: 87/87 archivos, 367/367**, sin NG05xx en los logs |
| `git diff --check` | limpio |
| Servicios del usuario | 4200 (PID 82535) y 8080 (PID 95661) intactos |

No repetí las sondas con Chrome (hidratación, 16 tamaños, CTA, UI): las cifras de esas evidencias son de Codex. Lo que comprobé es que las evidencias portables están ligadas al mismo build y que las herramientas pasan sus pruebas.

## Dictamen
**R3 cerrada como prueba local.** Las cifras de Codex coinciden con las mías. Se mantienen sus límites: producción en CSR, F4 abierta, sin Lighthouse 85, sin Safari/iOS físico y sin host propio. Los residuales de scroll, loading y cambio de fuente quedan reportados.

**Para el usuario (sin hallazgo nuevo):**
1. **Costo sin beneficio todavía.** El inicial de marketing sube **+25.93 kB** (477.19 → 503.12 kB) por la hidratación y el *event replay*. Hoy producción es CSR: en `/` Angular pide hidratar, no encuentra estado y renderiza normal (en producción, sin aviso). Ese costo solo se compensa cuando se active el prerender. Si el prerender no se va a publicar pronto, conviene decidirlo antes de desplegar este código.
2. **Commit de R3 como punto de control** (lo autoriza el usuario). Debe incluir el código, `angular.json`/`package.json`/`package-lock.json`, `tsconfig.marketing.server.json`, `scripts/` nuevos, `evidencias/`, `respuestas/008-f4-m3-r3*`/`t2*`, `solicitudes/` y `revisiones/008-f4-m3-r3*`/`t2*`. **Fuera:** `AGENTS.md`, `revisiones/005.md`, `006.md`, `007.md` y `008-f4-rendimiento-plan.md`, que son cambios ajenos. Antes: búsqueda de secretos en lo que entra y `git diff --check`. Sin push ni despliegue.
3. **Siguiente paso de M3, el prerender productivo:** necesita la aprobación del usuario. Antes de desplegarlo, sigue obligatorio R2 (el sitio de Hosting separado para dominios de clientes) y el orden Hosting → Functions. Después viene la medición pareada 5+5 de M2 frente a M3 para la meta de 85.
