Revisé las correcciones de 008 F2. **Nada bloquea la integración**, pero el cambio de márgenes laterales (P3-7) introduce una regresión visible que conviene corregir antes de cerrar F2. Solo leí código: no ejecuté suite, build ni UI, y no escribí ningún archivo.

## Correcciones verificadas

| Hallazgo | Estado | Evidencia |
|---|---|---|
| P2-1 `aria-current` | ✅ | `marketing-nav.component.ts:24` (`active=signal('')`); `spec.ts:23` comprueba que hay 0 `[aria-current]` |
| P2-2 altura de la barra y anclas | ✅ en Home | `marketing-tokens.css:16,18`; el host y la barra usan el token (`.css:1,4`). Con `border-box`, el alto de 60px + inset incluye el `padding-top`. `#inicio` está en la raíz `.fluora-marketing` (`fluora-home.component.html:1`) |
| P2-3 / P3-4 CTA | ✅ | `shortLabel` en `marketing-offer.service.ts:18`, `min-width:120px` (`.css:7`). El `aria-label` empieza con el texto visible. Spec con nombre de 85 caracteres |
| P3-5 vertical | ✅ | Sin `.mobile-cta` ni `padding-bottom:85px`; hero móvil de 40px (`fluora-vertical.component.css:47`) |
| P3-6 condición duplicada | ✅ | `marketing-route.ts` lo usan `app.ts:32` y `marketing-nav.component.ts:28` |
| P3-8 reintento de la oferta | ✅ | `started=false` en el error (`:19`). Ojo: el reintento solo ocurre cuando otro componente vuelve a llamar `load()`, o sea al entrar a Home. Para F2 está bien |
| P3-9 gutter y Escape | ✅ | `ts:37-38,48` restaura el valor anterior; Escape devuelve el foco al `summary` (`ts:44`) |
| Ciclo Tab/Shift+Tab | ✅ | `trapTab` (`ts:69-78`) más el spec `:27` |

## Hallazgos nuevos

**P2-A. Franjas claras laterales en iPhone horizontal (regresión de P3-7)**
- **Dónde:** `app.css:130-131` contra `styles.css:124-127`.
- **Problema:** `main.marketing-public-surface` y el host del footer reciben `padding-inline: env(safe-area-inset-*)`, pero no tienen fondo propio. En un iPhone horizontal con muesca (más de 767px, unos 47px de inset) se ve el degradado claro del `body` a los lados del hero oscuro de Home y de las verticales, y también junto al footer. Antes de este cambio los fondos llegaban de borde a borde.
- **Corrección recomendada:** quitar el padding de `main` y del host del footer, y aplicar el inset en los contenedores internos. Por ejemplo, `.vertical-container{width:min(1160px, calc(100% - 48px - env(safe-area-inset-left) - env(safe-area-inset-right)))}`, y lo equivalente en los contenedores de Home y del footer de marketing. Así los fondos siguen de borde a borde y el contenido queda fuera de la muesca.
- **Mínimo aceptable:** dar `background:#0a0f1a` a ambos elementos. Pero en las secciones blancas de la vertical quedarían franjas oscuras, así que no lo recomiendo.
- **Validación:** UI real a 844×390 con inset lateral simulado en Home, en una vertical y en `/privacidad`. No debe haber franjas del color del `body`, y la marca y los textos deben quedar fuera del inset.

**P3-B. El token de altura no llega a las anclas de la vertical**
- **Dónde:** `fluora-vertical.component.html:4,28-29` y `.css:36`.
- **Problema:** `.vertical-page` no está dentro de `.fluora-marketing`. Por eso `#planes` no tiene `scroll-margin`, y `#como-funciona` usa 100px fijos. En móvil con muesca la barra mide entre 107 y 119px, así que `verPasos()` (`fluora-vertical.component.ts:34`) deja el título bajo la barra.
- **Corrección:** declarar `--fl-nav-height` (y su variante móvil) también en `.marketing-public-surface`, y en la vertical usar `scroll-margin-top: calc(var(--fl-nav-height,72px) + 18px)` para los `[id]`. No conviene añadir la clase `.fluora-marketing` a la vertical, porque eso le cambiaría la tipografía y los estilos de foco.
- **Validación:** en móvil con inset de 59px, después de `verPasos()` el `getBoundingClientRect().top` de `#como-funciona` debe ser mayor o igual a la altura de la barra.

**P3-C. El filtro de visibilidad de `trapTab` no ve ancestros ocultos**
- **Dónde:** `marketing-nav.component.ts:73`.
- **Problema:** `getComputedStyle(e).display` no detecta un ancestro con `display:none`. Hoy no afecta, porque todo lo condicional del diálogo usa `@if`. Pero el día que se oculte un bloque por CSS, el "último control" sería invisible y el ciclo se rompería.
- **Corrección:** usar `e.checkVisibility?.() ?? e.getClientRects().length>0`.

**P3-D. Faltan pruebas de dos correcciones**
- No hay prueba de que `scrollbarGutter` vuelva a su valor al destruir el componente, ni de que Escape con el `<details>` abierto devuelva el foco al `summary`.
- Son dos asserts en `marketing-nav.component.spec.ts`: uno en el caso `:26` y uno nuevo con `keydown` Escape sobre `document`.

**Nit:** el nombre accesible queda redundante en "Probar: Probar 11 días". Cumple WCAG 2.5.3, así que es opcional. Podría usarse `label()` cuando ya empieza con `shortLabel()`.

## Alcance

No encontré cambios fuera de F2 en estos archivos. La lógica de `App` para el tenant y el host propio no cambió respecto de lo que ya aprobé. El scroll-spy se pasa a F4 como dice el plan, y no se publica ningún `aria-current` falso.

## Pendiente antes de cerrar F2
- Resultado de la suite final con las 3 pruebas nuevas, y el build de producción.
- UI real de escritorio y tableta a 1200, 1280 y 1350px (el CTA de 120px no debe empujar los enlaces).
- iPhone horizontal (P2-A).
- Anclas tras una carga completa con hash, por ejemplo de una vertical a `/#planes`. El router no tiene `anchorScrolling` y Home no procesa el fragmento, así que depende de que el navegador reintente el scroll después de renderizar. Está sin verificar.
