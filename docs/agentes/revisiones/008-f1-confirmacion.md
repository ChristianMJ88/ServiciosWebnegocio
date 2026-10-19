# 008 F1: P2-A corregida con opción 1

**Veredicto:** cierro F1 con una sola condición: que la suite final salga verde. No toqué la app ni otros documentos, y no hice commit ni despliegue.

## P2-A: queda resuelta
- **Leases separados:** en `src/app/core/marketing/marketing-appearance.service.ts:7-8` los contadores `users` y `safeUsers` son independientes. `activate(safeArea=false)` está en `:11`. Al liberar, la lógica es simétrica: `viewport` vuelve cuando `safeUsers` llega a 0 (`:24`) y `theme-color` cuando `users` llega a 0 (`:25`). Llamar dos veces a release no tiene efecto (`:23`).
- **Home:** `src/app/pages/fluora-home/fluora-home.component.ts:22` llama a `activate()` sin `true`, así que solo cambia `theme-color`. En host propio la función no hace nada. Se libera en `ngOnDestroy` (`:274-275`).
- **Nav:** `src/app/components/marketing-nav/marketing-nav.component.ts:39` usa `activate(true)` y libera en `DestroyRef` (`:48`). Busqué `app-marketing-nav` y solo aparece en su propio selector (`:12`), así que no está cableado y en F1 nada activa `cover`.
- **Punto de partida:** `src/index.html:20` no trae `viewport-fit` y no hay meta `theme-color`. Al liberar, `theme-color` se elimina, que es lo correcto.
- **Migración:** F1 no la necesita, coincide con lo que indicas.

## P3-B/C: resueltas en los specs
- **Servicio (4 casos):** restauración exacta de `viewport` (`spec:6`), activación compartida y retirada de metas que no existían (`:7`), `activate()` sin `cover` (`:8`), y `viewport-fit=auto` al principio, que se normaliza y luego vuelve a la cadena original (`:9`). El filtro de `:18` divide por comas, recorta espacios y quita `viewport-fit` en cualquier posición.
- **Home:** el destroy libera `theme-color` (`fluora-home.component.spec.ts:16`) y en host propio no se activa nada (`:17`).
- **Opcional, no bloquea:** el spec de Home no comprueba que `viewport` siga igual. Ese caso ya lo cubre el spec del servicio (`:8`), así que una línea en `:16` solo protegería contra que alguien pase `true` por error en Home.

## P3-D: sigue abierta
La combinación hover+focus y los rangos de pesos dependen del CSS nuevo de F2/F3. No la doy por resuelta. Al integrar el nav en F2 hay que comprobar que compensa los insets (`env(safe-area-inset-*)`), porque solo con él `cover` pasa a estar activo.

## Para cerrar F1
- Ejecutar la suite completa: deben pasar las 212 anteriores más las 4 nuevas del servicio y las 2 de Home.
- Ejecutar el build de producción.

Si cualquiera de las dos falla, F1 sigue abierta.
