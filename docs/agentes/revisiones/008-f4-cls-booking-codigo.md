**No encontré bloqueantes.** El cambio sigue el diseño de `revisiones/008-f4-cls-booking-diseno.md` en los cuatro archivos de Agendar y en el script. Solo leí código; no ejecuté nada.

## Cómo se cumple el diseño

| Punto del diseño | Dónde | Resultado |
|---|---|---|
| Bandera nueva de un solo sentido | `booking.component.ts:124` | Solo hay `set(true)` en `:892`, `:899`, `:923` y `:929`. Ni `onBranchChange` ni `handleSuccess` (`:863-866`) la tocan. |
| Sin sucursal elegida (lista vacía o varias) | `:891-893` | Se revela y no se llama a `getServices`. |
| Fallo al cargar sucursales | `:895-900` | Se revela y se ve el error que ya existía. |
| Servicios recibidos para la sucursal actual | `:909-923` | Se revela después de `hidratarServiciosDesdeQuery()` y `selectCatalogGroup`. |
| Fallo al cargar servicios para la sucursal actual | `:925-929` | Se revela después de la guarda de sucursal. |
| Respuesta de otra sucursal | `:910`, `:926` | El `return` temprano corre antes del `set`. |
| Nada en `finalize` | `:907` | El `finalize` solo toca `loadingCatalog`. |
| Shell | `.html:29` | Lleva la clase `--pending`, `inert` y `aria-busy`. El formulario se queda en el DOM porque no hay `@if`. |
| Estado para lectores de pantalla | `.html:26` | Está fuera del shell, con `visually-hidden` y `role=status`, y queda vacío al revelar. |
| CSS | `.css:1420-1422` | Solo `visibility: hidden`, sin tocar alturas. |

**Pruebas (`spec.ts:22-87`):** cubren los casos 1 a 8. El caso 5 comprueba que `selectedServices` ya está hidratado en el mismo paso en que se revela (`:36-37`). La prueba de respuesta obsoleta (`:59-69`) va más allá del diseño y también es correcta. No cambia ningún argumento esperado de `cotizar` ni de `reservar`.

**`--allow-quote`:** está apagado por defecto (`lighthouse-tenant-cache.mjs:21`). Solo permite un POST con la ruta exacta `/api/v1/publico/citas/cotizar` (`public-preview.mjs:30`). Cualquier otro método distinto de GET/HEAD sigue devolviendo 405 (`:31`), y lo que no esté bajo `/api/v1/publico/` devuelve 403 (`:33`). Queda registrado en `summary.json` como `allowReadOnlyQuote` (`:110`), y el hash del script se incluye en `scriptHashes` (`:118`). No abre ninguna otra escritura.

## Puntos menores (P3, no bloquean)

1. **Falta la prueba de DOM (caso 10, opcional en el diseño).** Ninguna prueba comprueba la clase, `inert` ni `role=status` en la plantilla. Si montar el componente con fixture no es viable por FullCalendar, conviene dejarlo escrito en la respuesta de Codex.
2. **El shell podría no revelarse si algo falla dentro de `next`.** Si `hidratarServiciosDesdeQuery()` o `selectCatalogGroup` lanzan una excepción, `:923` nunca se ejecuta y el shell queda oculto. Hoy no veo cómo podría pasar. Es el mismo riesgo que ya está registrado para cuando el servidor no responde nunca; no hace falta cambiar nada ahora.
3. **El aviso de carga puede no leerse.** El texto de `role=status` ya está presente al montar la página, y algunos lectores de pantalla no leen el contenido inicial de una zona `role=status`, solo los cambios posteriores. Es aceptable porque el shell oculto ya no es accesible y no hay contenido engañoso.
4. **`aria-busy` en el shell no tiene efecto,** porque `visibility:hidden` ya lo saca del árbol de accesibilidad. No hace daño; puede quedarse.

## Límites

- No apruebo métricas: no existe ninguna serie que se haya medido con la bandera.
- La serie anterior (Servicios, Inicio y Agendar, interrumpida antes de Checkout) **no** se atribuye a este cambio.
- La medición con servicio precargado y `--allow-quote` es solo un dato informativo. Debe guardarse en una carpeta y un `summary.json` propios, separados de la medición de Agendar sin parámetros.
- Si queda CLS por las oleadas de precios, profesionales o días, se documenta y se trata en un diseño aparte, sin tocar la lógica ahora.
- Siguen pendientes las comprobaciones del diseño: el LCP antes y después, el residual de 1440 medido con rectángulos, el footer fuera del viewport en las tres anchuras y que `booking-cart-column` desaparezca de las fuentes de desplazamiento.
- La suite completa aún no termina; esta revisión no garantiza que pase.
