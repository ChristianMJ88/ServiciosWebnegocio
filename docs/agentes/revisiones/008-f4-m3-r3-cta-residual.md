**Sí.** El movimiento interior del CTA del Nav se puede tratar como residual de contenido. Sigue contando en el CLS total y la muestra solo se reclasifica si se cumplen todas las condiciones de abajo. Si falla una, sigue siendo violation y el proceso termina con `exit 1`.

Solo usé Read y Grep sobre PROTOCOLO, `008-f4-m3-r3-cierre-parcial.md`, `marketing-nav.component.{html,css}` y `marketing-offer.service.ts`. No ejecuté nada ni leí los JSON en curso.

## Por qué se acepta

- **El texto no viene libre de la API.** `marketing-offer.service.ts:19` elige entre dos literales del front: `'Probar'` si `diasPrueba` es verdadero y `'Empezar'` si no. La API solo decide cuál de los dos se muestra. Es más acotado que la etiqueta del hero aceptada en r3d.
- **El desplazamiento coincide con un centrado.** `.action` es `inline-flex` con `justify-content:center` (`marketing-nav.component.css:1`) y `.top-cta{min-width:120px}` (`:7`). Si las dos palabras caben en 120 px, la caja exterior no cambia y el contenido se recentra. El icono se mueve la mitad de la diferencia de ancho entre las etiquetas (6.125 px de 12.25 px), con `dy=0` y el tamaño igual.
- **Su aporte es casi nulo.** Calculado con la fórmula estándar y un viewport de ejemplo de 1200×800: unión ≈19.1×20.8 px y distancia 6.125/1200, unas 2·10⁻⁶ por evento. Casi todo el 0.001726 viene del hero CTA. Pido la cifra exacta en el punto 5 de datos.
- **No hay nada que reclasificar ≤767 px.** El CTA tiene `display:none` (`:4`).

## Condiciones para reclasificar (todas, por muestra)

1. **Identificar por nodo, no por clase.** El único `source` permitido en la región actions es el elemento `.top-cta > i.bi-arrow-up-right`. Cualquier otro `source` del Nav sigue siendo violation.
2. **La caja exterior no cambia.** El rect de `.top-cta` (x, y, w, h) antes y después de la actualización difiere ≤0.5 px.
3. **El resto del encabezado no cambia.** Lo mismo (≤0.5 px) para `.menu-trigger`, `.desktop-login`, `.desktop-links`, `.brand` y `.top-actions`.
4. **El desplazamiento es el de un centrado.** `|dx_icono − (wTexto_antes − wTexto_después)/2| ≤ 0.5`, `dy=0` y el tamaño del icono igual. El ancho del texto se mide con `Range.getBoundingClientRect()` sobre el nodo de texto, antes y después.
5. **Sin fuentes estructurales.** 0 en parser, 0 en fuente y ningún otro `source` de hidratación fuera de los residuales ya aceptados (hero CTA e iconos de Pillars).
6. **El total sigue igual.** `initial.cls ≤ 0.01` en `normal`, sin restar nada. El aporte del icono se reporta aparte como `contentResidual.navCta`, solo informativo.

## Datos mínimos que faltan

1. **Las dos etiquetas con las dos fuentes.** Medir el ancho de la etiqueta y de la caja con `'Empezar'` y con `'Probar'`, en fallback y en Manrope, en 1200–1350 y en ≥1351.
   - El caso justo es 1200–1350, donde el padding baja a 12 px (`:2`). El espacio libre es 120 − 24 − 2 = 94 px, así que necesita `wTexto + 10 + 13 ≤ 94`, es decir, etiqueta ≤71 px.
   - Si en fallback alguna etiqueta pasa de 71 px, la caja crece durante el font swap y la condición 2 falla. Eso deja de ser residual de contenido.
2. **Tres fixtures derivados** que anulen solo `diasPrueba`, como `--logo null`:
   - `diasPrueba>0`: Empezar → Probar.
   - `diasPrueba=0`: sin cambio; se espera 0 `sources` en el Nav.
   - `plans503`: queda en Empezar; se espera 0.

   Así se ve que el residual depende solo del cambio de etiqueta.
3. **Reprocesar el `normal` de 32 con el P1-b corregido.** El CLS no cambia (lo da el PerformanceObserver), pero la clasificación previa usaba el filtro que no veía hijos. El «32/0 violations» no está acreditado hasta reclasificar sus `sources` crudos, o repetir la corrida si no se guardaron.
4. **Probar que ya existía.** `marketing-offer.service.ts` y `marketing-nav.component.css` están modificados en el working tree. Hay que mostrar que `shortLabel` y `min-width:120px` son iguales en HEAD, o que los `sources` del BEFORE reprocesados también contienen el icono. Sin eso, «ya existente» es solo una afirmación.
5. **El aporte exacto del icono** por muestra (impact fraction × distance fraction), junto al total del evento.

## Lo que no se acepta

- Excluir el nodo o la clase sin las condiciones 2–5.
- Reservar ancho, ocultar el CTA hasta que responda la API o cambiar el texto. No hace falta: la caja ya está reservada por `min-width`, y si el punto 1 de datos falla, se decide aparte.

Lo demás que comentas (Pillars a 22 px sin compensar con `min-height`, la separación de P2 y el control negativo de `details` en 1280 que coincide con el BEFORE) no contradice lo ya aprobado. Lo reviso cuando terminen el AFTER de 52 y las 32 finales.

No hay aprobación final. No autorizo commit, push ni deploy. No tengo herramienta de escritura: si se quiere conservar, hay que guardar esto como `revisiones/008-f4-m3-r3-cta-nav.md`.
