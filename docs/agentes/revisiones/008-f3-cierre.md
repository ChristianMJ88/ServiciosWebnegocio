# Revisión 008 F3: correcciones y cierre

**Veredicto: el código de F3 puede pasar a F4.** No encontré hallazgos altos ni medios. Los 3 medios y 5 de los 6 bajos de la revisión anterior están corregidos en el código. B3 queda como seguimiento de F0, con su motivo. F3 se cierra cuando Codex registre en ESTADO.md los resultados de la suite y del build finales, que siguen en curso.

No ejecuté pruebas, build ni navegador. Los números de la suite, el build, la matriz y el largo móvil los tomo de lo que reportó Codex, sin comprobarlos yo.

## Correcciones confirmadas

- **M1, lead del hero:** `fluora-home.content.ts:6` dice «Reservas y ventas en tu sitio. Equipo y cobros en una app.». Lo aprobó el usuario y no lo decidió Codex.
  - La medición coincide con la respuesta: D5 y §2 ocupan 3 líneas a 375 px. El texto aprobado ocupa 2 líneas (46.5 px).
- **M2, `aria-busy`:** ya no se usa en `fluora-home.component.html:1`. `dataReady()` solo aparece en el `when` de los 4 `@defer`.
  - Como `on viewport` y `when` funcionan como OR, el desplazamiento normal no depende de que respondan las APIs.
  - `publicSupport` convierte los errores en `null` (`platform-contact.service.ts:20`) y la navegación marca `verticalsReady` también cuando falla, así que ningún error deja el home bloqueado.
  - La prueba está en `spec.ts:26`.
- **M3, specs del home:**
  - La espera de planes y diferidos antes de posicionar el ancla está en `spec.ts:25`.
  - La cancelación con `wheel` está en `:26`.
  - Que no se importa GSAP en móvil ni con reduced-motion está en `:27`.
  - En escritorio hay una sola animación y `ngOnDestroy` la revierte (`:28`).
- **B1:** los 4 diferidos tienen `prefetch on idle`.
- **B2:** se usa `gsap.to` con `yoyo` y `clearProps` sobre `.fl-home-preview-icon`, que está en el hero (siempre cargado). Parte del estado actual, no hay `from` ni salto.
- **B4:** el footer de marketing en escritorio usa 12 px en `footer.component.css:252` y `:268`. El footer de tenant (`:298-304`) no cambia.
- **B5:**
  - El preview (`hero.component.html:10`) y las flechas (`plans.component.html:2`) usan `role="group"`.
  - Los pilares usan `h3` bajo un `h2` con `visually-hidden`, una clase que existe porque Bootstrap se carga de forma global (`styles.css:2`).
- **B6:**
  - Las flechas dependen de `canScroll()`, que se calcula con un ResizeObserver que se desconecta al destruir. El rail solo existe cuando hay planes, así que no aparecen durante la carga ni tras un error.
  - Los módulos se muestran sin `limiteUso`.
  - El paso 1 dice «Publica… con agenda y tienda».
- **Prueba gratuita condicionada a la API:** tanto el hero (`:7`) como el FAQ de precio (`faq.component.ts:12`) solo mencionan la prueba cuando `diasPrueba > 0`.

## Observaciones bajas (no bloquean; se pueden atender en F4)

1. **La spec de GSAP depende del orden de los tests** (`spec.ts:27`). `animation.loaded` se compara contra el valor anterior, y la factory de `vi.mock` solo corre la primera vez que se importa el módulo. Si `:28` se ejecutara antes, `:27` no detectaría un import indebido. La aserción sobre `animation.to` sí es sólida. **Sugerencia:** comprobar que `loaded` vale 0 de forma absoluta, o reiniciar los módulos.
2. **Falta probar el orden inverso del ancla** (`spec.ts:25`): datos listos y diferidos todavía pendientes. Tampoco se comprueba que `scrollIntoView` se llame una sola vez. El código lo garantiza al poner `pendingAnchor=null`, pero falta la aserción. **Sugerencia:** responder las 3 APIs, comprobar que no hubo desplazamiento, luego `render()`, y usar `toHaveBeenCalledOnce`.
3. **Las flechas de planes no tienen spec** que compruebe que se ocultan durante la carga o tras un error. **Sugerencia:** un caso con `offer.loading()` o `failed()` que verifique que no existe `.fl-home-rail-controls`.
4. **Margen del largo móvil:** 5617 px frente a un límite de 5684 px (7 × 812) deja solo 67 px. Cualquier copy o plan que se añada puede pasarlo. Conviene volver a medirlo en F4 con el número real de planes publicados.
5. **B3:** el FAQ y el footer siguen mostrando el teléfono en E.164 y la zona IANA tal cual. Aceptado como seguimiento de F0. Debe quedar registrado como pendiente en ESTADO.md para que no se pierda.

## Pendiente antes de registrar el cierre de F3

- Suite Angular final en verde. La anterior fue 242/76.
- Repetición del build con exit 0, los 4 chunks diferidos y GSAP en un chunk propio.
- Que la respuesta 008-f3 deje de decir «repetición pendiente».

Quedan para F4: Lighthouse, perfil de CLS (también el del prefetch de B1), iOS físico y notch, llegada por fragmento con la API lenta en navegador real, y capturas de tenant `/e/:slug` antes y después.

## Alcance

Leí solo el home y sus secciones, el contenido, las specs, el CSS del footer y el servicio de contacto. No modifiqué la app ni las revisiones anteriores. No hubo backend, migraciones, datos, commit, push ni despliegue.

Esta sesión solo tiene herramientas de lectura, así que no pude guardar el texto. Si se quiere persistir, se puede copiar a `docs/agentes/revisiones/008-f3-cierre.md` o regenerar con `scripts/claude-review.sh`.
