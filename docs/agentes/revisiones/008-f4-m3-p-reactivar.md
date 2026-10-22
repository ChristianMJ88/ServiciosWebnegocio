Terminé la revisión, en solo lectura. Esta es la revisión de la reactivación de P y de la regla de P4.

# Reactivar P y repetir P4: alcance, guardas y regla

Leí el código, sin ejecutar nada. El working tree **ya tiene P restaurado**, así que reviso lo que hay, no solo la propuesta. No escribí el archivo en `revisiones/` porque en esta sesión no tengo herramienta de escritura. Codex puede copiarlo como `revisiones/008-f4-m3-p-reactivar.md`.

## 1. Alcance y guardas: sin bloqueantes

Comprobé que:
- **Un solo camino de build:** `angular.json:75-90` `marketing` lleva `server`, `outputMode: "static"` y `tsconfig.marketing.server.json`. No queda ningún perfil de prerender aparte; lo asegura `marketing-hydration-wiring.test.mjs:28-29`.
- **Envoltorio de diagnóstico:** `build-marketing-hydration-probe.mjs:5` usa la misma configuración con otra salida. `build-marketing.mjs:14-16` rechaza la contención en los dos sentidos, y la prueba lo cubre (`wiring.test.mjs:39-45`).
- **Manifiesto:** `build-marketing.mjs:31` escribe `shell:'prerender'`, y `check-hydration-probe.mjs:12` lo exige.
- **postbuild y predeploy:** `package.json:10` y `check-public-builds.cjs:15-17` llaman a `checkHydrationProbe(..., {production:true})`, que exige:
  - modo `production` y runtime igual al revisado;
  - solo la ruta `/`, el h1, el lead y el CTA;
  - estado limitado a `__nghData__`, sin `.map`;
  - solo dos HTML, y `index.csr.html` vacío y del mismo build (`csr-shell.js:41-47`).
- **Shell:** `prepare-csr-shell.mjs:13-16` no copia un home prerenderizado como shell.
- **Clientes:** `prepare-clientes-site.mjs:24,32` protege las rutas de salida y quita el `index.html`. `check-clientes-site.mjs:9,22` rechaza el h1 del home.
- **Guarda CSR:** `check-marketing-csr.mjs` sigue como herramienta y no está en el postbuild (`wiring.test.mjs:33`). Coincide con lo que propones.
- **Negativos:** `check-public-builds.test.mjs:12` cubre `csr-index`, `csr-manifest`, `empty-routes`, `extra-html`, `stale-assets`, `polluted-csr`, `runtime-drift`, `sourcemap`, `development` y `missing-manifest`.

| # | Prio. | Ubicación | Hallazgo | Propuesta |
|---|---|---|---|---|
| R1 | Baja | `scripts/performance/README.md:78-81` | Todavía dice «idéntico a index.html: el usuario decidió volver a CSR». Ya no es cierto. | Describir P: `index` prerenderizado e `index.csr.html` vacío del mismo build. Es solo documentación. |
| R2 | Baja | `check-hydration-probe.mjs` | Las guardas no revisan si existe `dist/marketing/server`. Hosting publica solo `browser/`, así que no tiene efecto. | Nada ahora; solo dejarlo anotado. |
| R3 | Info | `marketing-hydration-wiring.test.mjs:39` | La sangría empieza con un espacio. | Cosmético; no tocar. |

No vi cambios de P en backend, planes, RBAC, 012 ni migraciones.

## 2. Regla de P4: la confirmo, con dos textos que no aparecen en tu interpretación

La regla vigente es `008-f4-m3-p-diseno.md:63-64`. No encontré ninguna regla de decisión posterior que la sustituya: la revisión `retorno-csr` y la `012-verificacion` no cambian el criterio. Lo que sí hay son dos textos anteriores que siguen vigentes:

1. **`008-f4-rendimiento-plan.md:47`**: la meta F4 incluye «**sin el chunk de Header en la red del home**». El §5 no lo repite, pero tampoco lo anula. Debe ser un criterio más: `headerRequested:false` en las 5 corridas de P.
2. **`008-f4-m3-diseno.md:89`**: dice «CLS ≤ 0.01 en **todas** las muestras». Así se entiende «las 10» del §5: las 10 corridas medidas, sin los warmups. La candidata es P, por lo que la meta se decide con sus 5 corridas. Si alguna corrida CSR pasara de .01, se informa como anomalía del control, pero no aprueba ni descalifica a P. En la serie anterior las 10 cumplieron.

Así queda la regla, aplicada a **cada una** de las 5 corridas de P salvo la mediana:
- mediana de Performance ≥ 85, en entero y sin redondear a favor;
- A, BP y SEO en 100;
- CLS ≤ .01;
- el nodo LCP es `h1#home-title` o el lead;
- Header no se solicita.

Si falla algún punto, se reportan las cifras y **decide el usuario**, sin microajustes. Coincido en que el +11 no basta para concluir nada. Aunque P llegue a 85, la meta queda acreditada solo en el preview local, no en Hosting real, y el despliegue sigue sin autorizar.

**Riesgo concreto:** el CLS de P anterior fue .00977–.00978, a 0.0002 del umbral (resumen JSON, líneas 143-211). El build nuevo incluye 012 y puede pasarlo. Si ocurre, es un incumplimiento, no motivo para ajustar.

## 3. Condiciones para la serie

| # | Prio. | Condición |
|---|---|---|
| C1 | **Alta** | **Baseline:** antes de medir, comprobar que el CSR archivado tiene la huella `86a0c01127ae…` (la de la serie anterior). Si no coincide o se reconstruyó desde `4e93dd1`, es otra etiqueta y no hereda el 71. |
| C2 | Alta | **Candidato:** se registra su propia huella. No hereda el 82 de `da5fbb…` ni el `d4705a…` del retorno a CSR. Se mide con `--spa-shell csr`, como exigió `p3-codigo` A1. |
| C3 | Alta | **Serie abortada:** el orquestador corta ante un error de API o de auditoría (README:49-50). Si pasa, la serie incompleta se guarda y se repite **entera**, sin mezclar corridas de dos series. |
| C4 | Media | **Alimentación:** registrar `pmset -g batt`, `pmset -g` (lowpowermode) y `pmset -g therm` antes y después, en solo lectura. Si sale «AC attached» y a la vez «discharging», se anota tal cual, sin atribuirle causa. |
| C5 | Media | **Pausa de ng serve 4200:** verificar PID y comando, enviar `kill -STOP` y confirmar el estado `T` con `ps -o stat=`. Enviar `CONT` en `finally`/`trap EXIT INT TERM`, desde un shell que no sea el del orquestador, y comprobar después que volvió a `S`/`R` y que 4200 responde. Los procesos hijos (esbuild) quedan inactivos: no tocarlos. El 8080 y MySQL no se tocan. |
| C6 | Media | **Evidencia:** el resumen nuevo va en un bloque aparte, por ejemplo `p4Repeat`, con fecha, huellas, alimentación y los 2 warmups separados. No sobrescribe `baseline`, `prerender` ni `userDecision`. |
| C7 | Baja | **Angular:** que sea antes del commit y no condición de P4. Sin cambiar `testTimeout` ni workers. Mi 3×387/92 se hizo con CSR y no cubre la configuración P actual. |

Antes de medir, comprobar también:
- que hay 17 GiB libres;
- que no corre ningún otro build, suite ni revisión de Claude en paralelo;
- que los builds, la batería Node y la sonda de hidratación del artefacto P terminaron en verde. No los verifiqué yo, porque no ejecuté nada.

## Dictamen

Puedes medir P4 cuando se cumplan C1–C5. No hay bloqueantes de código. Sigue sin autorización para commit, despliegue ni creación de sitios, y N1/N2 de 012 continúan como condiciones de despliegue.
