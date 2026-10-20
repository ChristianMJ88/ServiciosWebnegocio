Revisé `app.ts`, `app.html`, `app.spec.ts` y `header.component.spec.ts`, y los comparé con `revisiones/008-f4-cls-codigo.md`. No encontré defectos en estos cambios y no hace falta ninguna corrección. No ejecuté nada.

## Contraste con los hallazgos

**P2 de cancelación transitoria: corregido.**
- `src/app/app.ts:66` ahora ignora las cancelaciones `Redirect` y `SupersededByNewNavigation` antes de marcar la navegación como resuelta. Las demás cancelaciones (las que no traen código) sí la marcan, y la prueba de `app.spec.ts:198` lo sigue cubriendo.
- La prueba de `app.spec.ts:168-178` cubre ambos códigos. Comprueba que `navegacionResuelta()` siga en `false` y que el contenido siga pendiente hasta el `NavigationEnd` siguiente. La prueba anterior, que daba por bueno el comportamiento contrario, ya no está.
- En una carga en frío de `/e/x` que se redirige a `/`, ya no se ve el Header genérico durante el tiempo intermedio.

**P3, huecos en las pruebas: cerrados.**
- **Marketing → tenant** (`app.spec.ts:180-194`):
  - En marketing no se crea el bloque diferido.
  - Al entrar en el negocio, la página de la ruta sigue montada mientras el contenido queda pendiente.
  - Cuando el chunk termina, se muestra el Header del negocio y nunca el de Fluora.
- **Fallo del chunk sin negocio** (`app.spec.ts:156-166`): con el negocio inactivo, el enlace de `@error` apunta a `/`. Es una ruta válida porque `routeFor()` sin slug devuelve `/` (`tenant-context.service.ts:150-153`). El contenido se muestra y `inert` se retira.
- **Header en host propio** (`header.component.spec.ts:11-32`):
  - Comprueba que se use el nombre del negocio, que no aparezca Fluora y que `tenantHomeActiva` coincida con lo que el sistema calcula hoy.
  - Acepto tu precisión: `homeLink()` = `routeFor()` = `/e/slug` también en host propio (`header.component.ts:45`). Por eso, en `/`, `tenantHomeActiva` vale `false`. Es un contrato anterior a esta tarea y queda fuera de su alcance.

## Observación opcional (P3, no es una corrección)

En `header.component.spec.ts:26`, el valor esperado `isHome` se calcula a partir de `homeLink()`, que es justo lo que se está probando. Así, la aserción de las líneas 27 y 29 pasa con cualquier valor que devuelva `routeFor()`.
- Si se quiere detectar un cambio futuro en ese contrato, conviene fijarlo de forma explícita: `true` para `/e/negocio-prueba…` y `false` para `/` en host propio. Un comentario puede remitir al contrato vigente de `routeFor()`.
- Solo vale la pena si se toca ese archivo por otra razón.

## No apruebo (pendiente)
- La repetición limpia de la suite completa después de ampliar la cobertura. La corrida de 276/81 es anterior.
- El build final que acredite otra vez que el Header sale en un chunk separado. La evidencia de `chunk-D47RL4UG.js`, de 35,70 kB, es del primer build.
- El CLS en frío y en caliente en 390, 820 y 1440, con trazas, y Home85.
- La comprobación en las trazas de que el LCP final sea contenido real y no el `<p>` "Preparando el sitio…".
- Sigue fuera de alcance el frame en el que, entre dos negocios, el Header compara la ruta de A con el `homeLink` de B. Hay que verlo en la traza de la navegación SPA.
