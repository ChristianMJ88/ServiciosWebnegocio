# 008 F4 M3 T2: revisión del cambio de pruebas

Autor: Claude Code, 2026-10-20. Esta revisión es de solo lectura. No corrí suites ni builds, no modifiqué archivos y no leí `.env`, Postman ni credenciales.

**Qué leí:**
- La solicitud `solicitudes/008-f4-m3-t2-codigo.md`.
- El diseño `revisiones/008-f4-m3-t2-diseno.md` y la respuesta `respuestas/008-f4-m3-t2.md`.
- Las tres specs actuales.
- `vitest.config.mjs`.
- En `site-promotions.component.ts`, la línea de `providers`.
- `caja-payments-section.component.html:55-84`.
- La evidencia `/private/tmp/fluora-m3-t2-series-1.json`.

**Qué no leí:** los nueve logs de D0 ni los cinco logs de la serie.

**Límite de la revisión:** con solo Read, Glob y Grep no pude comparar contra `HEAD`. Por eso verifiqué la cobertura contra el inventario de casos y aserciones de mi diseño y contra el total: 10 casos (3 + 2 + 5), que coincide con el focal 10/10.

## Veredicto

**Aprobado. No hay hallazgos que bloqueen.** T2 se puede cerrar con la serie ya registrada y R3 puede empezar. No hay commit de R3, push ni despliegue autorizados.

## Revisión por archivo

### `site-promotions.component.spec.ts`
- **Override:** `overrideProvider(SitePromotionsFacade, …)` (`:15`) cubre bien el proveedor de nivel componente (`site-promotions.component.ts:19`: `providers: [SitePromotionsFacade, { provide: MAT_DATE_LOCALE, useValue: 'es-MX' }]`).
  - Ya no queda `overrideComponent`, así que se elimina la recompilación JIT.
  - `MAT_DATE_LOCALE` real se conserva.
- **Locale:** el assert de `:29` se lee con `fixture.debugElement.injector`, que es el inyector del host e incluye los providers del componente. Comprueba de verdad el valor es-MX del componente, no uno de raíz.
- **Llamada de guardado:** `toHaveBeenCalledOnce()` (`:25`) va antes de leer `mock.calls[0]`, como pedía el diseño.
- **Aserciones previas:** las cuatro siguen:
  - ID 42;
  - campos expandidos;
  - campos colapsados;
  - valor `Oferta editada`.
- **Aislamiento:** el `beforeEach` crea un `guardar` nuevo y un TestBed nuevo en cada caso.
- **Control del override:** si la facade real se colara, fallaría con `NullInjectorError` de `HttpClient`, que es una señal explícita.

### `booking-shell.spec.ts`
- **Estructura:** dos `describe` con `beforeEach(async)`.
  - Subjects y `cotizar` se crean de nuevo en cada caso (`:32-33`, `:101`).
  - `DeferBlockBehavior.Manual` se mantiene en los dos grupos.
  - `setTenant` va solo en el grupo con selección precargada, como en el original.
- **Controles de suscripción:** están todos los que pedía el diseño:
  - `branches.observed` (`:56`, `:125`);
  - `staff.observed` (`:65`);
  - `cotizar` 1 vez y `quote.observed` (`:68`);
  - `nextQuote.observed` (`:85`).
- **Aserciones de comportamiento:** se conservan todas:
  - `inert` en cada fase y `aria-busy`;
  - `status` fuera del shell;
  - identidad de `.booking-flow` antes y después de resolver;
  - reintento con `toHaveBeenCalledTimes(2)` y la cotización igual a `result`;
  - texto neutro sin `$`;
  - `pendiente de confirmación`;
  - clase `--pending` y el `status` vacío en "sin selección".
- **Estados intermedios:** `whenStable` no espera a un Subject abierto, así que el estado `inert` con la cotización pendiente (`:67`) sigue verificado. La serie verde también confirma que `whenStable` no se bloquea en modo zoneless.

### `caja-payments-section.component.spec.ts`
- **Montaje:** `registerLocaleData` queda al nivel del módulo (`:10`). `render()` no cambia: sigue con `Object.assign` y `detectChanges`, sin `setInput`, así que no dispara `ngOnChanges`.
- **Contratos de clase:** los dos casos (`:35`, `:42`) siguen sin cambios y separados del DOM.
- **Assert nuevo con sentido:** en la plantilla, `.caja-promotion-breakdown` está **anidado dentro** de `.caja-payment-context` (`html:70-82`).
  - Antes, `breakdown == null` habría pasado aunque faltara todo el contexto.
  - Con el `not.toNull()` de `:68`, el nombre del caso ahora sí se cumple.
- **Comprobante:** que `.caja-payment-context` sea null (`:76`) sigue siendo coherente con `@if(!ventaCobrada())` (`html:69`).
- **Importes:** las 4 aserciones monetarias de `:57-60` siguen intactas.

## La declaración sobre el presupuesto de hooks es correcta

`vitest.config.mjs` solo fija `isolate: true`. `testTimeout` (5 s) y `hookTimeout` (10 s) quedan en sus valores predeterminados, así que la frase de la respuesta (`:41`) es exacta. Le faltan dos precisiones que no bloquean y que recomiendo copiar a ESTADO:

1. **Cada límite se aplica por separado.** El caso tiene su propio límite de 5 s y cada hook tiene 10 s, incluido el `afterEach` de teardown de Angular. Un caso de Booking podría tardar unos 15 s antes de fallar. El fallo seguiría visible, pero el umbral real es más alto que antes.
2. **El residual de 4214 ms no se puede repartir.** El reporter muestra hook + cuerpo juntos, así que no se sabe cuánto fue montaje y cuánto comportamiento. Debe quedar como *residual conocido de primer montaje en Booking*, sin garantías bajo otra carga. La respuesta ya lo dice así.

También estoy de acuerdo en no atribuir la estabilidad solo a `whenStable`, y en presentar lo de Promociones como un mecanismo coherente, sin llamarlo benchmark pareado.

## Evidencia de la serie

El JSON coincide con la tabla de la respuesta:
- 5 corridas consecutivas, todas con `exitCode: 0`, 85/85 archivos y 341/341 pruebas.
- Duraciones: 36.33 / 44.41 / 44.95 / 41.76 / 36.68 s.
- `over4s` solo trae una línea en la corrida 4: Booking `error: false`, 4214 ms.
- Promociones no aparece por encima de 4 s en ninguna corrida.

Esto es lo que Codex registró. Yo no corrí nada.

**La condición de cierre del diseño (§5) ya se cumple con la serie 1**, siempre que las specs no se toquen después. Cualquier edición posterior, incluidas las P3 de abajo, reinicia la serie de cinco.

## Hallazgos opcionales (P3, no bloquean)

Si se aplica alguno, hay que repetir la serie. Recomiendo dejarlos para cuando se toquen estos archivos por otro motivo.

| Prioridad | Archivo:línea | Hallazgo | Sugerencia |
|---|---|---|---|
| P3 | `booking-shell.spec.ts:34-48`, `:101-112` | La indentación del `beforeEach` no es consistente: el cuerpo de `configureTestingModule` queda menos indentado que el resto del hook. Es solo estético. | Reindentar más adelante. |
| P3 | `site-promotions.component.spec.ts:10`, `:15` | El mock tiene tipo `Observable<never[]>` y `useValue` no tiene tipo, así que si cambia el contrato de la facade, TypeScript no lo detecta. | `useValue: {…} satisfies Pick<SitePromotionsFacade,'cargar'\|'guardar'>` y tipar `guardar` con el retorno real. |
| P3 | `booking-shell.spec.ts:29` | `Mock<() => Observable<…>>` no refleja los argumentos de `cotizarCitas`. Ningún caso verifica el payload enviado a cotizar. | Si se amplía la cobertura, agregar `toHaveBeenCalledWith(expect.objectContaining({…}))`. No es una pérdida frente al original. |

## Validación que debe hacer Codex antes de cerrar (sin repetir la serie)

1. Confirmar con `git diff -- <3 specs>` que no se borró ningún `expect(`: el número por archivo debe ser igual o mayor que en `HEAD`. Registrar ese conteo en ESTADO, porque yo no pude verificarlo.
2. Actualizar ESTADO.md con:
   - T2 cerrado;
   - la serie 1 como evidencia;
   - el residual de Booking (4214 ms, hook + cuerpo, cubierto por el `hookTimeout` predeterminado);
   - la decisión pendiente del usuario sobre una tarea de aplicación para dividir `BookingComponent`.
3. Los cambios de T2 quedan en el working tree y no van en el commit de R3, salvo que el usuario autorice otra cosa. Nada de push ni despliegue.
