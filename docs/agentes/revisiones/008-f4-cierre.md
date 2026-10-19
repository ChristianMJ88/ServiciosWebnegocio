# Revisión 008 F4 (cierre): el código queda cerrado

No tengo hallazgos altos, medios ni bajos nuevos. Las cinco correcciones resuelven B1–B5 y no amplían el alcance. Antes de cerrar F4 del todo, Codex tiene que registrar en `ESTADO.md` el build de producción, Lighthouse/CLS, la repetición de la matriz UI, la navegación con red lenta y las capturas de tenant. Solo leí código: no ejecuté pruebas, build ni navegador. El resultado de 246/77 en verde es lo que reporta Codex; yo no lo comprobé.

## Verificación de B1–B5

- **B1 ✓ Una sola fuente para el margen.** `marketing-sections.directive.ts:24-26` calcula `line` con el `scroll-margin-top` computado de la raíz más 2 px. Solo usa el borde inferior de la barra cuando el CSS no da un margen válido.
  - La raíz es `<div appMarketingSections class="fluora-marketing" id="inicio">` (`fluora-home.component.html:1`). Coincide con `.fluora-marketing[id]` en `marketing-tokens.css:16`, la misma regla que usan las anclas hijas.
  - El 18 ya no aparece duplicado en el TS.
  - En móvil, el `env(safe-area-inset-top)` de `marketing-tokens.css:18` ya viene incluido en el valor computado.
  - Ahora la sección aterriza 2 px por encima de la línea en todos los anchos. Antes caía justo en el límite en móvil.
- **B2 ✓ Spec de `aria-current`.** `marketing-nav.component.spec.ts:31` comprueba tres casos:
  - `planes` marca 2 enlaces, los dos con `#planes`.
  - `como-funciona` marca 1 y ninguna pestaña móvil.
  - `''` no marca nada.
  - Coincide con el template (`marketing-nav.component.html:9,15,16,18`).
- **B3 ✓ No depende del orden del array.** `directive.ts:28-33` se queda con el mayor `top` que cumpla `top<=line+1`. La spec, en `:25`, pone `como-funciona` en 17 y `producto` en 8 y espera `como-funciona`, que va antes en el array. Así queda probado que gana la posición y no el orden.
- **B4 ✓ Resize agrupado y cancelado.**
  - `directive.ts:41-44` ignora los eventos mientras hay un frame pendiente.
  - `:51` cancela el frame al destruir.
  - La spec cubre los tres casos: dos resizes producen un solo RAF y ningún observador nuevo hasta el callback (`:29`); un resize pendiente se cancela con `123` al destruir (`:30-31`); un resize posterior no crea observadores (`:32`).
- **B5 ✓ Cadena de `rootMargin`.**
  - Con margen 78, `line` vale 80 y el margen inferior `innerHeight-81` (`:27`).
  - Al pasar el margen a 90 y hacer resize, `line` vale 92 y el margen inferior `innerHeight-93` (`:29`).
  - Ambos valores cumplen `innerHeight-line-1`, coherente con la franja de 1 px y con la tolerancia de `publish`. La spec ya no depende del 18 del header.

## Informativo (no bloquea)

1. La rama de respaldo (`bar.bottom`, sin CSS) no tiene spec. Es un camino que hoy no se usa en el Home, así que lo acepto como está.
2. `line` se toma de la raíz, pero las anclas aterrizan en los hijos. Hoy la misma regla CSS da el mismo valor a los dos. Si algún día una sección tiene su propio `scroll-margin-top`, los valores dejarían de coincidir.
3. Sigue vigente la nota de B4: `line` solo se recalcula con `resize`. Hoy no afecta al Home.

## Pendiente para cerrar F4 (no lo comprobé)

- Build de producción con exit 0 y los mismos chunks.
- Lighthouse local y un perfil de CLS que incluya el `prefetch on idle`.
- Matriz de 16 orientaciones. Debe incluir la validación de B1 a 375 px y a 1440 px: entrar por `/#planes` y pulsar cada enlace; debe marcarse la sección de destino. También debe confirmar que al final de la página en móvil sigue marcado «Preguntas».
- Capturas de tenant `/e/:slug` antes y después.
- Llegada por fragmento con la API lenta.
- iOS físico y notch, que siguen sin estar disponibles.

## Alcance

Leí:
- `marketing-sections.directive.ts` y su spec
- el template y la spec de `marketing-nav`
- la línea 1 de `fluora-home.component.html`
- los tokens `--fl-nav-height` y `scroll-margin-top`
- `solicitudes/008-f4-cierre.md`, `revisiones/008-f4.md` y `respuestas/008-f4.md`

No hubo escrituras, commit, backend, migraciones ni despliegue. Esta sesión solo tiene herramientas de lectura, así que para conservar la revisión hay que guardarla en `docs/agentes/revisiones/008-f4-cierre.md`.
