# 008 F4 M3 · R4: revisión de correcciones y cierre

Autor: Claude Code, 2026-10-20. Revisión hecha solo leyendo archivos. No ejecuté suites, builds, `git diff` ni `cmp`, y no leí `.env`, `postman/` ni credenciales. Las cifras de ejecución que aparecen aquí las reporta Codex. Mi revisión anterior sigue en `revisiones/008-f4-m3-r4-codigo.md`, sin cambios.

## Veredicto

**M1, B1, B2 y B4 quedan cerrados.** El código de R4 está aprobado. Lo que falta antes de entregar R4 son mediciones, no correcciones (§3). B3 y el SSR real quedan para R3.

## 1. Estado de los hallazgos

### M1 · Cerrado

- **Caso de servidor** (`marketing-server-shell.spec.ts:25-58`):
  - `forbidden` ahora es un `vi.fn` que registra cada llamada además de lanzar error.
  - Las aserciones ya no dependen de que se propague una excepción, sino de que el espía no tenga llamadas: `media`/`resize` en `:58` y `storage` en `:53`.
  - Esto también resuelve el problema de `FluoraHome.ngAfterViewInit` asíncrono (`fluora-home.component.ts:79-82`). Si el guard fallara, la llamada a `win.matchMedia` quedaría registrada aunque el error terminara como una promesa rechazada.
- **Control positivo** (`:66-82`):
  - `expect(document.defaultView).toBe(window)` (`:69`) demuestra que Nav (`marketing-nav.component.ts:44,50`) y Home (`fluora-home.component.ts:73-74`) llaman a los mismos objetos que se espían.
  - En el navegador, el control comprueba que se lee el storage, que se llama a `matchMedia('(min-width:1200px)')` y que se registran `scroll`, `click` y `keydown`. Es decir, en el caso del servidor los venenos sí están conectados.
- **El IO no notificante es aceptable** para la integradora (`:30,56`): el renderer de `@defer` en jsdom observa sus placeholders, y la prueba comprueba que no se observe ningún id del scrollspy. El veneno real del IO está en `marketing-sections.directive.spec.ts:10-19`, que tiene su control de navegador en `:20-43`. Ahí se ejercen el observer, el RAF y el cleanup.
- **Observación, sin acción:** el control solo espía `getItem`, mientras que el caso del servidor también envenena `setItem` y `removeItem`. Como el objetivo es el mismo `Storage.prototype`, alcanza.

### B1 · Cerrado

`marketing-render-context.spec.ts:25-29` comprueba que app da `false`, marketing `true` y tenant `false` con origen idéntico. La respuesta (§Hallazgos, fila B1) deja registrada la regla para el prerender productivo: orígenes HTTPS distintos y concordantes con runtime-config.

### B2 · Cerrado

`marketing-server-shell.spec.ts:59-62` comprueba:
- `scrollbarGutter` vacío;
- `data-tenant-theme` ausente;
- `--tenant-primary` sin aplicar;
- ningún `link[data-tenant-public-styles]`.

### B4 · Cerrado

`platform-host.service.spec.ts:36-49` cubre los espacios y barras finales del runtime, y también `location` y runtime ausentes. Este último caso documenta un comportamiento que ya existía: con host vacío se clasifica como tenant en el navegador. En el servidor ese camino queda cerrado porque se exige el contexto. El conteo cuadra: 9 casos antes del refactor, 11 al final.

## 2. Lectura directa del snapshot (último ajuste)

**El contrato se conserva** (`platform-host.service.ts:70-122`):
- `snapshot` es `private readonly` y se crea una sola vez por instancia en el inicializador del campo. Ningún método lo muta. En el servidor, sus valores salen de un contexto congelado.
- Que los métodos lean `this.snapshot.x` en lugar de copias equivale a lo anterior, porque las copias se tomaban del mismo objeto en el mismo momento.
- `marketingHostnameValue` y `appHostnameValue` siguen calculados una sola vez (`:73-74`).
- La firma pública no cambia. `resolvePlatformHostSnapshot`, `cleanOrigin` y `getHostnameFromOrigin` tampoco cambian.
- Un grep en `src/` no encuentra referencias a los campos retirados: solo aparecen `resolvePlatformHostSnapshot` y su spec.
- La inyección sigue en contexto de construcción. El servidor sin contexto lanza el error antes de clasificar.

**Validación mínima de este ajuste:**
1. Los specs focales `platform-host.service.spec.ts` (11) y `marketing-render-context.spec.ts`.
2. Una corrida completa normal posterior al cambio.

Las dos corridas de 341/85 ya reportadas son anteriores a este ajuste y no lo acreditan.

## 3. Lo que falta para entregar R4

No hay bloqueos de código. Faltan estas mediciones de Codex:

1. **Pruebas después del ajuste:** los specs focales y al menos una suite completa normal (Node 20.19, `TZ=America/Mexico_City`), con su conteo. Lo ideal son dos corridas consecutivas, igual que en la evidencia anterior.
2. **Builds y guardas:** los dos builds y `check-public-builds` en verde, y `index.csr.html` idéntico byte a byte a `index.html`. Hoy la respuesta dice «verificación en curso»; hay que reemplazar eso por resultados.
3. **Tamaño inicial de marketing:**
   - Si queda dentro de ±1 kB de 476.17 kB, se reporta la cifra y queda cerrado.
   - Si sigue un poco por encima, por ejemplo hasta unos +2 kB, **no recomiendo más ajustes de bytes**. El criterio de ±1 kB servía para detectar que entrara algo inesperado, como `platform-server`, SSR o código privado. Unas décimas de kB atribuibles a los guards, a `EMPTY` y al contexto son el costo esperado de R4. En ese caso hay que documentar la diferencia y su causa, y confirmar con `check-public-builds` que no entró ningún paquete de servidor. Yo lo aceptaría como desviación registrada.
4. **Actualizar la respuesta:**
   - el ajuste del snapshot como último cambio;
   - las corridas posteriores, separadas de las dos anteriores;
   - el tamaño final.

   Mantener los límites declarados: el servidor está simulado en jsdom, no se certifican Domino/SSR ni la hidratación, B3 queda para R3 y el contexto no tiene proveedor.

## 4. Para R3 (sin acción en R4)

- **B3:** comparar los valores por defecto de `:root` (`src/styles.css:24-45`) con los que aplica `App`, y medir el primer pintado antes y después del bootstrap.
- **SSR real:**
  - con `@angular/platform-server`, comprobar que no hay NG05xx y que el prerender es estable;
  - repetir la integradora con `ngServerMode` real, sin el IO no notificante que hoy requiere jsdom.
- **Guarda de prerender productivo:** exigir orígenes HTTPS distintos y concordantes con runtime-config.

Después de cerrar §3, detenerse para la revisión del usuario antes de R3. Sin commit de R4, push ni despliegue.
