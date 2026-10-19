## Revisión 008 F4, pie de página durante el resolver: recomiendo aprobarla

Solo leí archivos: no edité nada, no hice commits ni toqué backend, migraciones o despliegues. **No hay hallazgos P0, P1 ni P2.** Queda una nota P3 sobre un comportamiento que cambia.

### Alcance: correcto y mínimo
- `src/app/app.html:10` solo añade `&& (esNavegacionMarketing() || tenantContext.activo())`. Las dos señales ya existían:
  - `esNavegacionMarketing` está en `app.ts:31`.
  - `TenantContextService.activo` está en `tenant-context.service.ts:11`.
- No se introducen reglas de host, rutas, permisos ni negocio. `footer.component.*` no cambia en esta corrección.
- **Cierra la causa del P2 de `revisiones/008-f4-rendimiento-cierre.md:29`.** Antes, el footer se montaba con `!tenantActivo()` y aplicaba `site-footer--marketing` (`footer.component.html:1`), lo que podía pedir las fuentes de marketing en `/e/*` o en un host propio. Ahora, en esos contextos, el footer solo se monta con el tenant activo, y entonces esa clase siempre es falsa.

### Compatibilidad por contexto

| Contexto | Antes | Ahora |
|---|---|---|
| Marketing o plataforma (`/`, `/acceso`, legales en plataforma) | Footer Fluora | Igual, porque `esNavegacionMarketing()` es verdadero |
| `/e/:slug` mientras carga el resolver | Footer Fluora con fuentes de marketing | Sin footer |
| `/e/:slug` cuando el resolver responde | Footer del tenant | Igual |
| `/e/:slug` con error o sin publicar | Redirige a `/` | Redirige a `/` y se pinta el footer Fluora por `esNavegacionMarketing` (`tenant.resolver.ts:20-23`) |
| Host propio mientras carga el resolver | Footer Fluora | Sin footer |
| Host propio con error | Footer Fluora | Sin footer (`tenant-host.resolver.ts:27-29`). Es mejor, porque no aparece la marca Fluora en un dominio ajeno |
| Paneles internos | Sin footer | Igual, por `mostrarChromePublico()` |

El WhatsApp flotante y el chatbot ya exigían `tenantContext.activo()` (`app.html:15` y `:23`), así que el footer ahora sigue el mismo criterio que el resto del chrome del tenant.

### P3: legales cargados directamente en un host propio
`app.routes.marketing.ts:54` y `:59` (`privacidad` y `terminos`) no usan `tenantHostResolver`. Si alguien abre `https://dominio-propio/privacidad` como primera página, el tenant nunca se activa y la página queda **sin footer**. Antes mostraba el footer Fluora. Si se llega navegando desde el sitio del tenant, el contexto se conserva porque `app.ts:121` no lo limpia en un host propio, y el footer aparece.
- **No bloquea:** es preferible a mostrar la marca Fluora en ese dominio.
- **Si se quiere footer ahí,** sería un cambio aparte: añadir `canMatch: [tenantHostMatch]` y el resolver a las rutas legales del host propio. No va en este alcance.

### Pruebas
- Los tests de `app.spec.ts:35-48` cubren lo que se pide:
  - Ausencia del footer en `/e/x` y en un host propio mientras falta el contexto.
  - Montaje sin `.site-footer--marketing` cuando llega `setTenant`.
  - En `/e/x`, ninguna llamada a `/publico/plataforma/soporte`.
- **Huecos menores, opcionales:**
  - No hay un caso `/e/x` + `setTenant`. Usa la misma condición que el de host propio, así que basta con el actual.
  - Ningún test comprueba que el footer siga presente en una página de plataforma distinta de `/`, como `/privacidad`. Hoy lo cubre en parte `app.spec.ts:33`.
- No ejecuté la suite. Las 252 pruebas en 78 archivos aprobadas las reporta Codex y no las verifiqué.

### Validación propuesta para Codex
1. Terminar el build de las dos entradas y registrar en `respuestas/008-f4.md` los hashes y tamaños de los chunks iniciales.
2. En Chrome incógnito, pestaña Network:
   - En `/e/<slug>` y en un dominio propio, comprobar que no se pide ningún `fonts/marketing/*.woff2` ni el chunk de `fluora-home`, y que el footer aparece al resolver.
   - En `/` y `/privacidad` de la plataforma, comprobar que el footer Fluora sigue presente.
3. Revisar el CLS en `/e/<slug>`. Como el footer se monta tarde, queda bajo el contenido y no debería mover el área visible, pero conviene confirmarlo en Lighthouse.

Coincido en no dar por cumplida la meta de Performance 85. Siguen pendientes las hojas de Google Fonts y animate.css desde CDN en `index.html:26` y `:28`, ya señaladas en la revisión anterior.
