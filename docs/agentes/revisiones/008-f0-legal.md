Ya revisé el diseño, la página legal y `application.yml`. Este es el resultado.

## Revisión 008-F0, adenda de contacto legal

**Veredicto: apruebo la adenda con 2 cambios (L1 y L2) y 3 ajustes menores.** V140 sigue libre: la migración más alta es V139. Se mantienen A1, A2, M2 y M3 de `008-f0-diseno.md`. Como dijiste, no escribí ningún archivo. Si quieren conservar esta revisión en el canal, se puede pegar en `docs/agentes/revisiones/008-f0-legal.md`.

### Lo que confirmo
- **La misma fila, con `correo_legal VARCHAR(150) NOT NULL`.** Una sola `version` y una sola auditoría cubren las dos partes. Si alguien guarda soporte y otra persona guarda el contacto legal con la misma versión, una de las dos recibe 409. Es lo que queremos, porque el PUT reemplaza la fila completa.
- **Ruta pública:** `GET /api/v1/publico/plataforma/contacto-legal` queda abierta por la regla `ConfiguracionSeguridad.java:40`. No la bloquea ninguna regla de `FiltroLimiteSolicitudes`. Devuelve `{"correo": "..."}`:
  - 200 con `max-age=60, public`;
  - 404 si no hay fila, sin caché pública;
  - no depende de `visible`.
- **Siembra de `contacto@refluora.com` en V140:** es correcta, porque es la dirección que ya se publica hoy.
- **Sin datos:** se muestra el aviso de contacto no disponible y no se usa ninguna dirección de respaldo.
- **`application.yml`:** las líneas 170 y 227 quedan fuera de F0 y no se cambian alias, salidas ni credenciales. Ojo con un detalle: `correo-bienvenida.soporte` (línea 170) se llama literalmente «soporte» y vale `contacto@`. Va a quedar distinto del soporte configurable (`soporte@`). Anótalo como residual para un seguimiento, no lo cambies en F0.

### Hallazgos

**L1 (alta). El texto restante de la página de eliminación cita un correo que puede no existir.** `legal-page.component.html:95` dice «escribiendo al mismo correo». Si el contacto no carga, la frase apunta a algo que no se muestra.
- *Corrección:* cuando no hay datos, esa frase va en un `@if` y se sustituye por el mismo aviso de contacto no disponible. Es el único texto que cambia además de los cuatro enlaces (líneas 16, 34, 69 y 80).
- *Corrección:* el enlace de la línea 80 conserva el asunto: `'mailto:' + correo + '?subject=' + encodeURIComponent('Solicitud de eliminación de datos')`.
- *Corrección:* mientras carga se muestra un marcador neutro. El aviso de no disponible solo sale después de un 404 o de un error, para que no aparezca un instante al cargar.

**L2 (media). `@Email` permite crear un `mailto:` que añade destinatarios.** El validador de Hibernate acepta `?`, `&` y `=` antes de la arroba. Con un valor como `a?bcc=x@dominio.mx`, el `mailto:` queda con destinatario `a` y una cabecera `bcc`. Solo un administrador de plataforma puede capturarlo, pero el valor se publica en páginas legales y en el pie de página.
- *Corrección:* además de `@Email`, validar `correo` y `correoLegal` con `^[a-z0-9._%+-]+@[a-z0-9-]+(\.[a-z0-9-]+)*\.[a-z]{2,}$`, después de `trim` y de pasar a minúsculas.
- *Prueba:* `a?bcc=x@dominio.mx` devuelve 400 en los dos campos.

### Ajustes menores
1. **Nombre de la tabla:** como la fila ya no es solo de soporte, recomiendo llamarla `plataforma_contacto` en V140. Hoy no cuesta nada; después costaría otra migración. Las rutas de administración `/plataforma/marketing/soporte` pueden quedarse como están. Si Codex prefiere `plataforma_soporte`, no lo bloqueo.
2. **No uses un `CHECK` para exigir minúsculas.** Con la intercalación `_ci`, `correo_legal = LOWER(correo_legal)` siempre es verdadero, así que el `CHECK` no sirve. La normalización se hace solo en Java.
3. **Aviso en el editor:** la sección «Contacto legal» explica que el correo se publica en Privacidad, ARCO, Términos y Eliminación de datos, y que no es el de soporte. Avisa también que cambiarlo modifica el contenido del aviso de privacidad, pero la fecha «Última actualización» (líneas 14, 43 y 75) no cambia sola. Si el usuario quiere que esa fecha se mueva, es una decisión suya fuera de F0.

### Lo que ya existía y no entra en F0
- Las rutas `/privacidad`, `/terminos` y `/eliminacion-de-datos` no tienen `tenantHostMatch` (`app.routes.marketing.ts:53-67`), así que en un host propio muestran el documento de Fluora. Que ahí aparezca el contacto legal de Fluora es coherente, porque el documento es de Fluora. A2 no aplica a LegalPage.
- `revisiones/007.md:164` propone escribir fijo `contacto@refluora.com` en el escalamiento de reclamos. Cuando se implemente, debe consumir este contrato.

### Contenido de V140
- La tabla con las columnas de `008-f0-diseno.md` §3, más `correo_legal VARCHAR(150) NOT NULL`.
- `CHECK (id = 1)`.
- La FK `actualizado_por` a `plataforma_usuario(id)`, NULL en la siembra.
- Una sola fila sembrada con los valores de soporte de `008-decisiones.md:31-36`, el teléfono `+522213688033`, `visible=TRUE` y `correo_legal='contacto@refluora.com'`.
- Se reserva en ESTADO.md después de `git fetch --all --prune`.

### Pruebas (además de las que ya anotó Codex)
**Backend:**
- `visible=false`: soporte devuelve 404 y contacto legal 200. Sin fila, los dos públicos y el GET de administración devuelven 404.
- El JSON del contacto legal tiene una sola clave, `correo`.
- Cabeceras: el 200 lleva caché pública de 60 s y el 404 no la lleva.
- PUT sin `correoLegal`, inválido o con el caso de L2 → 400. Un valor en mayúsculas se guarda en minúsculas.
- Si el PUT cambia solo `correoLegal`, la `version` aumenta y la auditoría registra `anterior` y `nuevo` de ese campo.
- Concurrencia MySQL: dos transacciones con la misma versión, una cambia soporte y la otra el contacto legal. Resultado: 200 y 409, con una sola auditoría.
- Token de tenant contra la ruta de administración → 403. Con autorización o cabeceras de tenant, la ruta pública devuelve igual el valor de la plataforma.
- Migración V140 en una BD efímera con `fluora_tests`:
  - la fila sembrada tiene `correo_legal`;
  - un UPDATE a NULL falla;
  - el `CHECK` rechaza `id=2`;
  - Hibernate `validate` pasa.

**Angular:**
- Las tres páginas legales muestran el correo de la API en los cuatro enlaces, y el de eliminación conserva el asunto.
- Con 404 o error se muestra el aviso, el DOM no tiene ningún `mailto:` y la frase de la línea 95 queda reemplazada.
- `HttpTestingController`:
  - LegalPage nunca llama a `/soporte`;
  - el pie de página y el FAQ nunca llaman a `/contacto-legal`;
  - en un host propio o en `/e/x` no hay ninguna petición de soporte (A2).
- El editor envía `correoLegal` y muestra el `mensaje` del backend.

**Postman:** añadir el GET público de contacto legal a la colección como solo lectura. Las peticiones PUT solo corren en un entorno efímero, con las guardas de `scripts/test-postman-guards*.cjs`.

**Búsqueda en el repo (reemplaza el criterio M1):** `contacto@refluora.com` no debe aparecer en `src/` ni en `backend/src/main/java`. Solo se permite en:
- la siembra de V140;
- `application.yml:170,227`;
- `backend/README.md`;
- los fixtures de prueba.

Siguen vigentes las demás restricciones: no se editan migraciones ya aplicadas, el tenant queda intacto y no hay commit, push ni despliegue de 008.
