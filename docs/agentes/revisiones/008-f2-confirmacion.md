**F2 puede cerrarse y pasar a F3, con una condición:** hay que registrar que el build final de producción de marketing terminó con exit 0. La respuesta 008-f2 y ESTADO todavía lo marcan "en curso". Ese build es el único que ya incluye las últimas correcciones de CSS y fragmento, así que el "previo verde" no lo cubre. No encontré bloqueantes.

Solo leí código. No ejecuté la suite, el build ni la UI, y no escribí ningún archivo. Si se quiere dejar constancia, este texto puede guardarse como `revisiones/008-f2-confirmacion.md`.

## Hallazgos de 008-f2-cierre

| Hallazgo | Estado | Evidencia |
|---|---|---|
| P2-A franjas laterales | ✅ en código | `app.css:130-133`: `main` y el host del footer ya no tienen `padding-inline`. Los márgenes de la muesca se aplican dentro de cada contenedor: `fluora-home.component.css:1181`, `fluora-vertical.component.css:3,47`, `footer.component.css:234,276` y `legal-page.component.css:12,117`. Los fondos siguen de borde a borde. |
| P3-B token de altura en las verticales | ✅ | `app.css:130,132` declara `--fl-nav-height` en `.marketing-public-surface`, y `fluora-vertical.component.css:50` aplica `scroll-margin-top: calc(var(--fl-nav-height,72px) + 18px)`. En escritorio la barra termina en 80px (8 de `top` + 72) y el margen da 90px, que coincide con el 89.67 medido. En móvil da 78px + inset contra una barra de 60px + inset. |
| P3-C ancestros ocultos | ✅ | `marketing-nav.component.ts:79-86` usa `checkVisibility({checkVisibilityCSS:true})` y, si no existe, recorre los ancestros revisando `display` y `visibility`. |
| P3-D pruebas | ✅ | `marketing-nav.component.spec.ts`: gutter restaurado (`:26`), Escape devuelve el foco al `summary` (`:28`) y ancestro oculto (`:29`). |
| Ancla con carga completa | ✅ | `fluora-home.component.ts:182-186` usa `ActivatedRoute.fragment` más `afterNextRender` con el injector y busca solo dentro del host. Los enlaces de la barra son `href` completos, así que siempre pasan por este camino. |

## Observaciones que no bloquean

- **P3. El ancla se calcula una sola vez.** Si la oferta, el soporte u otro contenido que va antes de `#planes` llega por API después del primer render, el ancla puede quedar desplazada cuando la red es lenta. La medida de 89.67 se tomó en local. Conviene medirla con la red limitada en F4, junto con el scroll-spy y el CLS.
- **P3. Insets asimétricos.** `width: calc(100% - L - R)` con `margin-inline: auto` reparte el ajuste por igual a los dos lados. Funciona porque iOS informa insets simétricos en horizontal. Si algún día no fuera así, convendría usar `padding-inline` dentro del contenedor. No requiere cambio ahora.
- **Cobertura de pruebas.** En el entorno de pruebas se ejecuta el camino alterno de `visible()`. La rama `checkVisibility` solo quedó probada en el WebView real que reporta Codex. Es suficiente.
- **Nit opcional.** El nombre accesible redundante ("Empezar: Comenzar con…", `spec.ts:23`) sigue igual. Cumple WCAG 2.5.3.

## Límites que se mantienen

- No hay prueba en un iPhone físico ni con muesca simulada. P2-A queda verificado en el código, no en un dispositivo. Recomiendo agregar al checklist de F4 o de antes del despliegue una revisión a 844×390 en Safari o en el Simulador de iOS: Home, una vertical y `/privacidad`, sin franjas del color del `body`.
- No revisé el alcance de F3/F4 ni el resto del working tree, solo los archivos de F2 citados arriba. Tampoco hay commit, push ni despliegue.

**Validación pendiente para cerrar:** registrar en la respuesta 008-f2 y en ESTADO la repetición del build de producción con exit 0. Hecho eso, F2 queda cerrada.
