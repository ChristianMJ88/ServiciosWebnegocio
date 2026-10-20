Ya terminé de revisar los seis archivos. Además consulté `tenant.resolver.ts` y las rutas `e/:slug`, porque una duda sobre las cancelaciones de navegación lo requería. No encontré bloqueantes de corrección para D1–D3.

## Hallazgos

**P2. Una redirección cuenta como "navegación resuelta".** Está en `src/app/app.ts:62-66`.
- Cuando el slug no existe o no está publicado, `tenantResolver` devuelve un `UrlTree` hacia `/`. Eso produce un `NavigationCancel` de tipo Redirect, y luego un `NavigationEnd('/')`.
- Entre esos dos eventos, `navegacionResuelta` ya vale `true`, el negocio está inactivo y `publicPath` sigue en `/e/x`. Si el CSS ya cargó, se monta el Header genérico ("Fluora", no home), el contenido se muestra y en el siguiente evento todo cambia a la navegación de marketing. Es un parpadeo y un posible desplazamiento en una página que de todos modos se va.
- **Recomendación:** no marcar como resuelta una cancelación con `code === NavigationCancellationCode.Redirect` (ni `SupersededByNewNavigation`), o marcarla solo cuando `router.getCurrentNavigation()` sea `null`.
- **Prueba:** un `NavigationCancel` de tipo Redirect, sin negocio, deja `contenidoPendiente()` en `true` hasta el `NavigationEnd` siguiente.
- La prueba actual de `NavigationCancel` (`app.spec.ts:168-179`) da por bueno justo el comportamiento contrario. Hay que ajustarla.

**P2 (medición). El texto de carga ensucia FCP y LCP.** Está en `app.html:23-25` y `app.css:6-14`.
- "Preparando el sitio…" se pinta en cada carga en frío del negocio. Al ser `fixed`, no produce desplazamiento.
- Pero será el FCP y un candidato temprano a LCP. Si el hero tarda o es pequeño, el LCP puede salir artificialmente bajo.
- En las trazas, verifica que el elemento LCP final sea el del contenido y no este `<p>`.

**P3. Huecos en las pruebas**
- `header.component.spec.ts` solo cubre el slug. Falta el host propio, con `Location.path()='/'` y `homeLink()='/'`.
- Falta la entrada SPA marketing → `/e/x`. Ahí se crea un defer nuevo y el contenido se oculta hasta que llega el chunk. Es aceptable, pero debe quedar fijado en una prueba.
- La prueba de error (`app.spec.ts:156`) tiene el negocio activo. Si el negocio está inactivo, `routeFor()` en el enlace de `@error` debe dar una ruta válida. No lo comprobé.

## Conforme
- **D1** (`app.ts:31,55`, `header.component.ts:28`): usa la ruta real y conserva el negocio ya cargado en slug y en host propio. Hay prueba.
- **D2** (`app.html:7`): la condición es correcta. Entre negocios se conserva la misma instancia del Header (prueba `:181-201`).
- **D3:**
  - Las consultas usan string + `ElementRef` y no hay referencia a la clase del Header en TypeScript.
  - El outlet sigue vivo, con `inert` y `aria-busy` correctos.
  - Footer, WhatsApp y chat esperan a que termine la carga.
  - La prueba `:74-79` muestra que la clase se retira en un solo `detectChanges`.
- **Herencia de `visibility`:** fuera de `marketing-bootstrap.css` (generado), admin y caja, ningún CSS en `src` fuerza `visibility: visible`. La búsqueda no revisó estilos inline ni estilos aplicados desde JS.
- **El `@error` con enlaces en vez de un `div` vacío:** acepto el cambio. No inventa altura ni añade reglas de negocio.
- **`HeaderComponent` en `imports`:** se puede mantener, siempre que el build acredite que el chunk sale separado. Eso sigue pendiente.

## Límites
- No ejecuté nada.
- No apruebo métricas. Siguen pendientes la comprobación del chunk separado, la suite completa, el CLS frío/caliente en 390/820/1440 con trazas y Home85.
- Un chunk que se cuelga sin dar error deja el contenido oculto indefinidamente. Es un riesgo que ya acepté en el diseño.
- Hay un detalle anterior a este cambio y fuera de alcance: entre dos negocios, el resolver llama a `setTenant(B)` antes del `NavigationEnd`, así que durante un frame `tenantHomeActiva` compara la ruta de A con el `homeLink` de B. Conviene comprobarlo en la traza de la navegación SPA.
