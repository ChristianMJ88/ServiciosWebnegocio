# Revisión 012 §6: correcciones P1-A/P1-B y P2

No encontré bloqueantes de código pendientes. P1-A, P1-B, P2-C, P2-D y los dos P3 incorporados (Accept vacío y noindex del CSR) están bien implementados. El cierre todavía depende de verificaciones operativas, no de cambios de código. Trabajé en solo lectura, no ejecuté comandos y no verifiqué ninguna cifra de pruebas. No pude guardar esta revisión porque no tengo herramienta de escritura. El texto está listo para `docs/agentes/revisiones/012-seo-cierre.md` si Codex o tú lo copian.

## Hallazgos que quedan cerrados

**P1-A, canonical interno**
- `tenant-host.resolver.ts:24` llama `applyTenant(config, state.url)`.
- `seo.service.ts:15` quita la query y el hash, y `tenantUrl` (`:130-138`) genera la URL local.
- El spec (`tenant-host.resolver.spec.ts:22-25`) comprueba `/servicios?utm=x`, `/politicas` y `/` contra el `<link>` real del DOM.

**P2-D, fallo sin bucle**
- `resolver.ts:27-30` envía a `/sitio-no-disponible`. Esa ruta (`app.routes.marketing.ts:15`) no tiene resolver, así que no vuelve a pasar por `''`.
- `TenantUnavailableComponent` pone noindex y elimina el JSON-LD del tenant (`seo.service.ts:71-82`).
- El spec `:26-29` comprueba que hay una sola llamada.
- "Volver a intentar" es un enlace `/` que solo se activa cuando el usuario hace clic, así que no crea un bucle automático.

**P1-B, origen de la plantilla**
- `custom-domain-page.js:25-26` exige `https:` y rechaza credenciales, path, query y hash. Un valor vacío o `undefined` hace que `new URL` lance error y la Function responde 503.
- `:61` pasa `clientesOrigin.origin` al loader.
- `index.js:11,215` lee `FLUORA_CLIENTES_ORIGIN` y no recibe nada de la petición.
- Pruebas: `custom-domain-page.test.js:55-61`. El `deepEqual(seen, ['https://clientes…'])` también descarta que se use marketing o el host del visitante.
- La guarda `check-clientes-site.mjs:11-12,29` exige que el CSR y los assets sean idénticos byte a byte al build de marketing. El runbook (`:141-147`) agrega `hosting:clientes`, aclara que el despliegue no es atómico y no ejecuta nada.
- Los rewrites de marketing (`tenantPage`, `landingPage`, `directoryPage`) siguen usando `marketingOrigin`. Es correcto, porque ahí la plantilla y los assets salen del mismo despliegue.

**P2-C, rutas normalizadas**
- `:47-51` responde 301 a la ruta en minúsculas y sin barra final, conservando la query. Esto pasa solo después de que el backend valida el host, y antes de pedir el catálogo y de renderizar.
- El header nunca llega a un HTML 200 con mayúsculas. El `Location` usa un hostname validado y confirmado por el backend, así que no permite redirigir a otro dominio.
- Pruebas: `test.js:62-64`.

**Consumo inicial del bootstrap**
- `tenant-site.service.ts:47-54` exige que `__FLUORA_CUSTOM_HOST__` sea igual a `location.hostname` y que exista el tenant. Lo usa una vez y borra los dos globales.
- Si el host no coincide, consulta la API y deja el bootstrap sin usar (`spec:17-20`).
- `__FLUORA_CUSTOM_HOST__` se inyecta con `JSON.stringify` de un host que solo admite `[a-z0-9.-]`, así que no hay riesgo con `$` ni con `</script>`.

**Otros cambios**
- Si falta `Accept`, se trata como HTML (`:18`, `test:65`).
- `X-Robots-Tag` en `/index.csr.html` (`firebase.json:243-255`) y la guarda lo exige (`check-clientes-site.mjs:32`).
- Cuando el host es un alias, el canonical apunta al `dominioPrincipal` que devuelve el backend (`test:23-26`).
- Revisé la aritmética del ejemplo de coste en `dominios-propios.md:35`: USD 0.0000127125 por invocación y USD 0.127125 por 10,000 cargas. Es correcta y está marcada como supuesto.

## P3, baja prioridad (no bloquean)

1. **Peticiones al dominio por defecto del sitio clientes.** Las peticiones a `<siteId>.web.app` y `<siteId>.firebaseapp.com` en rutas públicas llegan a `customDomainPage` y hacen una consulta al backend que siempre da 404. También es la ruta que usa la propia Function para descargar el CSR; ese archivo es estático y no la invoca, pero cualquier rastreador del dominio por defecto sí genera consultas.
   - **Sugerencia:** en `custom-domain-page.js:28-29`, si `host === clientesOrigin.hostname`, o es la variante `.firebaseapp.com` del mismo sitio, responder 404 noindex sin hacer fetch.
   - **Prueba:** `x-forwarded-host` igual al host de clientes, fetch que lanza error si se llama, y 404 esperado.
2. **Coste por navegación.** `dominios-propios.md:31` debería decir explícitamente que cada cambio de ruta SPA después del bootstrap vuelve a consultar `/publico/sitio/host/…`. Es 1 GET por navegación porque el resolver se ejecuta en cada ruta. Hoy queda implícito en `S`. Es la decisión aceptada y no requiere cambiar código.
3. **Mensaje de error en `csr-shell.js:53`.** Dice "Marketing CSR" aunque ahora también carga la plantilla de clientes. Es solo texto; no afecta los logs porque `log` registra únicamente `error.name`.
4. **`apiBaseUrl` acepta `http:`** (`custom-domain-page.js:27`). Probablemente sirve para el emulador. Conviene confirmar que la configuración de producción usa `https:`, o exigirlo fuera del emulador.
5. **Falta una prueba** de que `TenantUnavailableComponent` deja `robots: noindex, nofollow`. Hoy solo lo prueba el código de `SeoService`.

## Antes de cerrar (condiciones, no código)

- **Caso con un build de marketing distinto en el emulador.** Documentar que, después de cambiar el build de marketing, un asset con hash del dominio propio sigue respondiendo 200 desde clientes. Sin esto, P1-B solo queda probado con unit tests.
- **Registrar las tres corridas finales de Angular** con su log. Las cifras 381/90 son de antes de las últimas correcciones.
- **Cadena `X-Forwarded-For` real de Firebase.** El emulador no la reproduce. Queda como verificación previa al despliegue, como indica la solicitud, y no se debe dar por confiable la IP del cliente hasta comprobarla.
- **Requisito del runbook (`:131`).** El sitio clientes "aún no creado" debe existir y su origen debe confirmarse antes de definir `FLUORA_CLIENTES_ORIGIN`. Ese paso requiere autorización explícita.
- **Verificaciones P3/P4 y visuales** siguen en curso según la solicitud. Esta revisión no las cubre.

No se cambió la decisión de indexar `/checkout` y `/registro` ni el alcance del `apiProxy`. Quedan fuera de esta revisión, como estaba acordado.
