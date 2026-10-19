# Revisión 008-F1 cierre: aprobado con una condición (P2)

Solo leí y busqué archivos. No pude ver el diff, lanzar el build ni las specs, y no escribí la revisión en `docs/agentes/revisiones/`: esta sesión no tiene herramienta de escritura ni de terminal. Codex o el usuario tendrán que guardarla con `scripts/claude-review.sh` o copiándola a mano.

## P2-1 y P3-3/5: lo que quedó resuelto

- **Metas globales:** `src/index.html` ya no tiene `theme-color` ni `viewport-fit`. Solo aparecen en `marketing-appearance.service.ts`. La SEO de tenant (`seo.service.ts`) no escribe ninguna de esas dos metas, así que no hay conflicto.
- **Servicio de apariencia** (`marketing-appearance.service.ts:10-28`):
  - Guarda los valores previos solo en la primera activación.
  - Cada liberación es idempotente (`active`).
  - Al liberar la última vista restaura el valor anterior, o borra la meta si antes no existía.
- **Límite del tenant:** en `fluora-home.component.ts:22` la activación es un no-op en host propio de tenant y se libera en `ngOnDestroy` (`:275`). La ruta `''` de `FluoraHome` (`app.routes.marketing.ts:52`) queda detrás de `tenantHostMatch` (`:16`), así que no se carga en host de tenant.
- **Foco** (`marketing-tokens.css:15`): hay un solo token. El `box-shadow` oscuro (`#04130e`) se ve en el hueco del `outline-offset`, por dentro del anillo verde, y así resuelve el contraste en las secciones claras. No queda ninguna regla de `focus` en `fluora-home.component.css`.
- **Anclas** (`marketing-tokens.css:16`): usan `calc(var(--fl-nav-height) + 18px)`.
- **P3-4:** queda aplazado a F3 como dice la solicitud. Se acepta.

## Hallazgos

**P2-A. `viewport-fit=cover` se activa en Home, pero Home no compensa las zonas seguras.**
- Ni `fluora-home.component.css` ni `header.component.css` usan `env(safe-area-inset-*)`. El footer solo lo usa en su variante `:not(.site-footer--marketing)` (`footer.component.css:310`).
- En iPhone en horizontal, el header y los bordes del hero quedan bajo el notch. El problema ya no toca al tenant, pero ahora es una regresión de marketing.
- **Recomendación** (en orden de preferencia):
  1. Que F1 active solo `theme-color` y deje `cover` para F2, cuando la navegación nueva añada los insets.
  2. O añadir ahora `padding-inline: max(var(--fl-gutter), env(safe-area-inset-left/right))` al header y a los contenedores de Home.
- **Validación:** el simulador de iOS (Xcode) en horizontal basta. No hace falta un dispositivo real.

**P3-B. Las pruebas no demuestran la "restauración exacta" del viewport.**
- `marketing-appearance.service.spec.ts:6` comprueba `.not.toContain('cover')`. Debería comprobar `toBe('width=device-width, initial-scale=1')`.
- Falta una spec de Home que verifique dos cosas: que el destroy libera la lease, y que no se activa nada si `isCustomTenantHost()` devuelve `true`.

**P3-C. Si el viewport empieza con `viewport-fit`, la regex deja una coma al principio.**
- La regex de `:15` necesita una coma delante. Con un valor como `viewport-fit=auto, width=…` queda `, width=…, viewport-fit=cover`.
- Hoy no ocurre, porque ningún index tiene ese valor. Conviene limpiarlo dividiendo por comas, sin regex.

**P3-D. Si el botón tiene a la vez hover y foco, se pierde el anillo oscuro.**
- `fluora-home.component.css:8` y `:652` (`.btn-primary:hover`) tienen más especificidad que el token (0,5,0 y 0,6,0 contra 0,3,0) y sustituyen su `box-shadow`. El verde se mantiene.
- Pasa poco con teclado. Se puede resolver en F2 con `:hover:not(:focus-visible)`.

**Nota de diseño, sin cambio.** Al restaurar se sobrescribe cualquier cambio que otro componente haga a esas metas mientras la lease está activa. Hoy no hay otro escritor. F2 debe seguir usando solo este servicio.

## Condiciones de cierre

1. Que la suite final, con las 2 pruebas nuevas, termine en verde. La solicitud dice que sigue en curso.
2. Resolver P2-A con la opción 1 o la 2, o que el usuario acepte el riesgo de forma explícita.

## Límites

- No hubo prueba en iOS real: la barra de iOS sigue sin probarse, como reconoce la solicitud.
- Tampoco comprobé Network en un host de tenant.
- No toqué backend, migraciones, tenant, commits ni despliegues.
- Los P3 pueden pasar a F2.
