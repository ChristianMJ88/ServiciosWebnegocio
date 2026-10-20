**Veredicto: apruebo el cierre de P2 «CLS del tenant» para el artefacto `aeab9d…`, con los límites que la respuesta ya declara.** No encontré ninguna cifra mal atribuida ni ninguna afirmación que vaya más allá de la evidencia. F4 sigue abierta por HomeMarketing85, y M3 queda fuera.

## Lo que comprobé

- **Cifras.** Comparé las 30 celdas de la tabla «Cierre de mediciones» (`respuestas/008-f4-cls.md:125-131`) con `verified-analysis.log:1-30` y coinciden todas, tanto en frío como en caliente. Los totales también cuadran: máximo .000469845 en las 24 sin parámetros y .005125707 en las 6 precargadas, con 0 errores de red y 0 saltos de `main`. El texto dice «<.001 en las 24» y «<.01 en las seis» (`:133`). No dice que las 30 queden por debajo de .001 ni habla de CLS 0 absoluto.
- **Atribución por perfil.** El SHA aeab9d, las 15 warmups (5 rutas × 3) y el perfil mobile/simulate también en 1440 están declarados. Las cifras de 887c, 5c793 y cbceea están separadas en sus propias secciones y no se reutilizan. Que D1 solo está simulado consta en `:53`, `:141` y en ESTADO.
- **LCP.** Ninguna muestra tiene como LCP `p.public-shell-loading`, que es «Preparando el sitio…» (`src/app/app.html:24`). En Checkout, el `section.checkout-state > p/h1` corresponde a los estados vacíos (`checkout.component.html:8,10`). El estado de carga «Preparando pago seguro…» usa `<strong>` (`:6`), así que tampoco es un mensaje de espera.
- **Calendario lazy.** `calendarLoaded:true` aparece solo en las 4 precargadas de 820 y 1440, como dice `:135`.
- **HomeMarketing frente a Inicio del negocio.** No se mezclan. «Inicio del negocio» es la ruta del tenant, y HomeMarketing85 se declara abierta (`:93`, `:141`, ESTADO:3) con la mediana de 82 atribuida a 41dd….
- **Rendimiento.** No se promete nada a partir de una sola pareja; `:17` y `:85` lo descartan de forma explícita. Las puntuaciones de rendimiento del log no se usan como argumento.
- **Límites de UI.** Host propio e iOS físico no están disponibles. La captura IAB en blanco se descarta, la captura cruda de Lighthouse se presenta como Chrome simulado y no se certifica equivalencia visual. La navegación SPA entre dos negocios no tiene muestra (`:91`). Esto cubre el pendiente de `revisiones/008-f4-cls-correcciones.md:31` como límite declarado, no como algo verificado.
- **Booking/Calendar.** Coincide con `008-f4-cls-precarga-confirmacion.md`: P2 y P3 están cerrados, y su pendiente de medición (820 sin `TypeError`, sin empeorar las 24) se cumple en el log.

## Hallazgos (P3, solo documentación; ninguno bloquea)

1. **Atribución a `dl.cart-details` no trazable en la fuente compacta.** En `cls-verified-analysis.json:2160/2240` están los dos scores (.002457294 y .002668413, que suman .005125707). Pero solo aparecen `node_id`, no el selector. La geometría es coherente con la columna derecha a 1440 (x=904, w=268). Conviene citar de qué audit o report sale el selector.
2. **El tamaño de muestra no se repite en el cierre.** «Una fría + una caliente por celda» solo aparece en `:53`, en la sección de 887c, y ESTADO dice «30 muestras» sin decir n=1 por celda. Bastaría añadirlo en una línea.
3. **La corrección del calendario no se ejercitó en 390 en Lighthouse.** En 390 precargado el chunk no se carga (`calendarLoaded:false`), así que la corrección de `afterRenderEffect` solo se midió en 820 y 1440, además de en las pruebas unitarias. No es un defecto: es un límite que conviene dejar dicho.

## Lo que no puedo aprobar sin más evidencia

- **Header lazy (`chunk-LU24KJR6.js`, 35.70 kB)** y **Angular 301/83, tres builds, Functions 11/11 y preview 3/3**: solo los conozco por lo que declara Codex. No ejecuté nada ni hay salidas de esas corridas en lo que revisé. El cambio de nombre del chunk respecto a QBK6PX3V, con el mismo tamaño, es coherente con un cambio de hash.

## Pendientes reales

- La comprobación final de limpieza, que ESTADO:3 aún marca como pendiente.
- Seguimiento menor del residual del carrito de escritorio en precarga (.005), sin rediseñar ahora.
- Sin muestra todavía: navegación SPA entre dos negocios reales, host propio e iOS físico.
- Siguen opcionales los P3 de las pruebas: la aserción tautológica de `header.component.spec.ts:26`, `onBranchChange` con personal pendiente y `toContain('pendiente de confirmación')`.
- HomeMarketing85 y M3 siguen fuera de este cierre.
