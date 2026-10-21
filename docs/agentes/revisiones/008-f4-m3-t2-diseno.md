# 008 F4 M3 T2: diseño para corregir el montaje DOM inestable (antes de R3)

Autor: Claude Code, 2026-10-20. Revisión de solo lectura. No corrí suites ni builds, no escribí código y no leí `.env`, Postman ni credenciales.

**Qué leí:**
- Las tres specs.
- `booking.component.ts`/`.html`, `site-promotions.component.ts`/`.html` y `caja-payments-section.component.ts`/`.html`.
- `caja-cobro.component.ts`, `vitest.config.mjs` y `angular.json` (`test`, `test-build`).
- En `node_modules`, solo para confirmar mecanismos: `@angular/build/.../vitest/build-options.js`, `@angular/core/fesm2022/testing.mjs` y `@vitest/runner/dist/index.js`.

## 1. Diagnóstico con evidencia

1. **Las pruebas se compilan con AOT.**
   - `test-build` usa `@angular/build:application` (`angular.json:116-127`), así que `compileComponents()` casi no hace trabajo en los componentes que no tienen override.
   - **Caja** ni siquiera lo llama (`caja-payments-section.component.spec.ts:13`).
   - En **Booking** su costo debería ser marginal. El costo de Booking y Caja está en el primer `createComponent` + `detectChanges`: la primera pasada de creación de la plantilla, la inyección de estilos en jsdom y el código de Angular/Material que corre por primera vez en un entorno aislado nuevo (`isolate: true`, `vitest.config.mjs:5`), todo con 82 archivos compitiendo por CPU.
   - Esto coincide con lo que reportó Codex: el primer caso de cada archivo es el lento y los siguientes son rápidos (Booking: 6234 → 1967 → 1350 ms).
2. **Promociones paga además una compilación JIT real dentro del `it`.**
   - `overrideComponent(..., {set:{providers}})` agrega el componente a `pendingComponents` (`testing.mjs:591-594`). Después, `compileTypesSync()` llama a `compileComponent(declaration, metadata)` (`testing.mjs:740-753`), es decir, **vuelve a compilar con JIT toda la plantilla**, con los módulos de Material, en cada caso.
   - Es la única de las tres specs donde la hipótesis de "compileComponents dentro del it" se sostiene.
   - Evidencia extra: el caso `:8` nunca vuelve a llamar `detectChanges()` después de `agregar()`, así que el DOM que monta es casi vacío. Aun así tardó 5463 ms, lo que apunta al JIT y al calentamiento, no al render.
   - `overrideProvider` sigue otro camino: solo parcha `providersResolver` (`testing.mjs:612-645` y `806-811`) y no recompila nada.
   - Detalle: `set: providers` también quita `MAT_DATE_LOCALE: 'es-MX'` del componente (`site-promotions.component.ts:19`). Hoy la prueba corre con un locale distinto al real.
3. **Los stubs de hijos no sirven aquí.**
   - Para cambiar los `imports` de un componente standalone hace falta `overrideComponent`, y eso dispara la misma recompilación JIT del padre. Sería *más* costoso.
   - `errorOnUnknownElements: true` está activo (`build-options.js:56`) y `NO_ERRORS_SCHEMA` está prohibido.
   - Por eso **descarto los stubs** en las tres specs.
4. **Mover el montaje a `beforeEach` sí cambia el límite que aplica, y hay que decirlo.**
   - En Vitest, la duración que se reporta de un caso incluye `beforeEach` y `afterEach`: `start` en `index.js:1621`, hooks en `:1645`/`:1676`, `duration` en `:1736`. La métrica de "caso > 4 s" sigue viendo el costo.
   - Pero cada hook corre con su propio `hookTimeout`, que por defecto es de 10 000 ms (`vitest/dist/chunks/coverage.BuJUwVtg.js:2795`), y no con los 5000 ms del test.
   - **En Booking y Caja esto no reduce el costo: pasa el montaje de un presupuesto de 5 s a uno de 10 s.** Se justifica como separación entre fixture y comportamiento (es el patrón estándar de TestBed y ese costo es igual en todos los casos del archivo). No es una corrección de rendimiento y no debe presentarse así.
   - Con lo que permite T2 (sin cambiar app, workers ni timeouts), no hay otra forma de quitar el calentamiento del primer montaje. La única alternativa real que lo reduce es dividir la plantilla de `BookingComponent` (654 líneas y 1683 de CSS entre sus tres hojas). Eso es un cambio de aplicación y queda fuera de T2.
5. **`whenStable` no bloquea en estas specs.**
   - No hay zone.js; la app es zoneless. En las tres specs no aparecen `PendingTasks`, `pendingUntilEvent` ni llamadas HTTP reales.
   - Los `setTimeout` de `booking.component.ts:571,702,708,714,874` no bloquean `whenStable` en modo zoneless.
   - Esperar después de cada emisión es seguro y no usa timers reales.

## 2. Paso previo D0 (lo corre Codex, medición sin cambios)
Correr cada uno de los tres archivos solo, 3 veces (`ng test --include <ruta>`, Node 20.19, `TZ=America/Mexico_City`), y anotar la duración de cada caso.
- Si el primer caso, corriendo solo, tarda ≪ 2 s, el excedente en la suite completa es contención y calentamiento: se confirma el punto 4.
- Si tarda > 4 s incluso solo, es costo propio del montaje y se registra como residual que se resuelve fuera de T2.

Sirve como línea base para comparar después del cambio.

## 3. Diseño por archivo

### P1 · `site-promotions.component.spec.ts` (corrección real del costo)
```ts
describe('Editor de promociones', () => {
  let fixture: ComponentFixture<SitePromotionsComponent>;
  let guardar: Mock<(payload: any) => Observable<PromocionSitioAdmin[]>>;
  beforeEach(async () => {
    guardar = vi.fn((_payload: any) => of([]));
    TestBed.configureTestingModule({ imports: [SitePromotionsComponent] });
    TestBed.overrideProvider(SitePromotionsFacade, { useValue: {
      cargar: () => of({ promociones: [], servicios: [], sucursales: [], media: null }), guardar
    } });
    await TestBed.compileComponents();
    fixture = TestBed.createComponent(SitePromotionsComponent);
    fixture.detectChanges();
  });
  it('sends the existing promotion ID when saving edits', () => { /* mismo cuerpo desde const editor=… */ });
  it('expande el editor con fechas…', async () => { /* mismo cuerpo desde const editor=… */ });
});
```
- Cambiar `overrideComponent` por `overrideProvider` evita el JIT. Además conserva `MAT_DATE_LOCALE: 'es-MX'` real; ninguna aserción depende del locale.
- Agregar `expect(guardar).toHaveBeenCalledOnce()` antes de leer `mock.calls[0]`, para que si la llamada no ocurre, el fallo lo diga claramente.
- Se mantienen la aserción del payload con ID 42, la expansión con `whenStable`, el colapso y el valor del título.
- Control de que el override aplicó: si se colara la facade real, fallaría con `NullInjectorError` de `HttpClient` (la spec no registra `provideHttpClient`). Es una señal de error explícita.

### P1 · `booking-shell.spec.ts`
- Pasar `service` y `result` (datos puros) y el objeto del tenant a constantes del módulo.
- Dividir en dos `describe`, cada uno con su `beforeEach(async)`:
  - `configureTestingModule` igual al actual, con `DeferBlockBehavior.Manual`, `compileComponents()` y `setTenant` (solo en el primero);
  - `createComponent` + `detectChanges()`;
  - `shell = …querySelector('.booking-shell')`.
- Los Subjects y `cotizar` se crean de nuevo en cada `beforeEach` (variables `let` del `describe`). El teardown por caso no cambia: `getCleanupHook(true)` en `build-options.js:40`.
  - **A · "con selección precargada"**: `it.each([false, true])` con el cuerpo actual desde `const flow = …`.
  - **B · "sin selección"**: el cuerpo actual desde `const form = …`.
- Controles de suscripción, que **se agregan** sin quitar ninguna aserción:
  - antes de `branches.next`: `expect(branches.observed).toBe(true)`;
  - antes de `staff.next`: `expect(staff.observed).toBe(true)`;
  - antes de resolver `quote`: `expect(cotizar).toHaveBeenCalledTimes(1); expect(quote.observed).toBe(true)`;
  - después de `reintentarCotizacion()`: `expect(nextQuote.observed).toBe(true)`.
- Después de cada emisión, `click()` o `reintentarCotizacion()`, usar `fixture.detectChanges(); await fixture.whenStable();` antes de las aserciones del DOM. Los estados intermedios (inert mientras la cotización está pendiente) siguen verificándose, porque `whenStable` no espera a un Subject abierto.
- Se conservan:
  - la misma plantilla y la identidad de `.booking-flow`;
  - `inert` y `aria-busy`;
  - `status` fuera del shell;
  - el texto neutro sin `$`;
  - el reintento con `toHaveBeenCalledTimes(2)`;
  - el calendario diferido en modo manual.

### P2 · `caja-payments-section.component.spec.ts`
- Llamar `registerLocaleData(localeEsMx)` una sola vez, al nivel del módulo.
- `describe('contrato de clase')`: los dos casos de `runInInjectionContext` (`:33`, `:40`), sin cambios.
- `describe('DOM')` con `let vista: HTMLElement;` y un `describe` anidado por escenario, cada uno con su `beforeEach(() => { vista = render(...) })`:
  - `render(50)` → "presenta los importes del backend…" (las 4 aserciones actuales);
  - `render(0)` → "conserva el contexto… sin bloque de promoción". Recomiendo agregar `expect(vista.querySelector('.caja-payment-context')).not.toBeNull()`, porque el nombre promete algo que hoy no se verifica;
  - `render(0, true)` → el caso del comprobante, sin cambios.
- `render()` **no cambia**: sigue con `Object.assign` y el mismo `detectChanges`. Descarté montar un estado base y aplicar `setInput` después: dispararía `ngOnChanges` (`caja-payments-section.component.ts:116-122`) y el camino de estado ya no sería el mismo.
- Igual que en Booking, en Caja el `beforeEach` solo cambia el límite (punto 4). No acelera el render.

## 4. Lo que no se hace
- No se cambian `testTimeout`, `hookTimeout`, workers, `isolate` ni el código de la aplicación.
- No se usan `NO_ERRORS_SCHEMA`, stubs, `beforeAll` de "calentamiento" (ocultaría más), `sleep` ni timers reales.
- No se quita ni se debilita ninguna aserción.

## 5. Criterio de cierre de T2
- D0 registrado.
- 5 suites normales consecutivas en verde, con las mismas condiciones de la reproducción. Para cada una se anotan:
  - pasadas/total;
  - archivos;
  - duración;
  - todo caso > 4 s, sabiendo que la duración ya incluye `beforeEach`.
- Cualquier fallo reinicia la serie.
- Si después del cambio algún caso de Booking o Caja sigue por encima de 4 s (hook + cuerpo), se registra como **residual conocido**: un costo del primer montaje cubierto por el `hookTimeout` predeterminado, no como problema resuelto. El usuario decide si se abre una tarea de aplicación para dividir `BookingComponent`.
- Si Promociones sigue por encima de 4 s después de cambiar a `overrideProvider`, la hipótesis del JIT queda refutada y se informa.
- R3 no empieza hasta que T2 esté cerrado.
