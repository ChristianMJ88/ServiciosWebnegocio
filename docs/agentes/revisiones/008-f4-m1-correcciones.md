# Revisión 008 F4 M1: confirmación de las correcciones

Solo leí archivos. No ejecuté builds, suites ni Lighthouse. Esta sesión no puede escribir archivos, así que el texto queda listo para guardarlo como `docs/agentes/revisiones/008-f4-m1-correcciones.md`, con autoría de Claude.

**Veredicto:** el hallazgo Alta (keyframes) y el Media (límite de tiempo) están cerrados en el código. Los cinco hallazgos Bajos también. Encontré un defecto Bajo nuevo en la procedencia de Lighthouse y un caso límite Bajo en el respaldo de inserción. Ninguno bloquea la medición de M1, pero conviene corregir el primero antes de guardar los resúmenes.

## Lo que quedó cerrado

**Alta (spinner sin animación): cerrado.**
- `scripts/css/marketing-subset.mjs:52-58` recoge los nombres de cualquier valor de una regla que se conserva. Eso incluye `--bs-spinner-animation-name`.
- `src/styles/generated/marketing-bootstrap.css:8` ya contiene `@keyframes spinner-border` y `placeholder-glow`.
- La comprobación del propio generador (`marketing-subset.mjs:72-76`) casi no puede fallar, porque `select` ya conserva todo keyframe que esté en `animations`. La protección real es `check-public-builds.cjs:17`, que revisa el archivo final, y con eso basta.

**Media (navegación colgada): cerrado.**
- `tenant-public-styles.ts:30-34`: al cumplirse el límite pone `ready=true` y deja pasar la navegación. No quita el link ni vacía `pending`, así que `ensure()` devuelve la misma promesa y no repite la petición.
- Si el CSS llega tarde, se marca como cargado. Si falla tarde, lo quita y deja reintentar.
- El spec cubre 999 ms y 1000 ms con tiempo falso, que siga habiendo un solo link y la carga tardía (`spec.ts:43-60`).

**Bajos: cerrados.**
- **Reintento:** `ready` nunca vuelve a `false` (`spec.ts:39`).
- **Respaldo de inserción:** si no aparece la hoja global, registra un error y respeta el orden con el CSS crítico (`spec.ts:61-71`).
- **Prueba de la app:** usa un tenant activo con WhatsApp y chat, y comprueba los tres elementos flotantes y el footer antes y después de `ready` (`app.spec.ts:27-50`).
- **Orden de las corridas:** la secuencia medida es AB, BA, AB, BA, AB. Con 5 corridas, A va primero 3 veces, un desbalance inevitable con un número impar. Conviene mencionarlo en el resumen.
- **Tipo `.woff`:** agregado en `public-preview.mjs:13`.

## Defectos en las correcciones

| Pri. | Ubicación | Hallazgo | Corrección y prueba |
|---|---|---|---|
| Baja | `lighthouse-home.mjs:130-133` | **`measurementCode` no identifica al script que mide.** `commit` es el HEAD, pero `lighthouse-home.mjs` y `performance/public-preview.mjs` no están en git y además están fuera del pathspec de `sourceDirty`, que solo cubre `src`, `angular.json`, `package*` y `tsconfig`. El resumen diría «limpio» aunque el script haya cambiado, y el campo no cumple lo que promete. | Agregar a `measurementCode` el sha256 de `scripts/lighthouse-home.mjs` y `scripts/performance/public-preview.mjs`, o agregar `scripts` al pathspec y tratar los archivos sin seguimiento como cambios. **Prueba:** editar un comentario del script cambia el hash o pone `sourceDirty` en `true`. |
| Baja | `tenant-public-styles.ts:49` | **Caso límite del `??`.** Si la hoja global existe pero es el último hijo de `<head>`, `nextSibling` vale `null` y el código usa `componentStyle` en su lugar. Si ese `<style>` estuviera antes de la hoja global, el CSS del negocio quedaría antes que el global. Hoy no ocurre porque los estilos de componente se agregan al final en tiempo de ejecución. | Cambiar a `const before = global ? global.nextSibling : componentStyle;`. **Prueba:** con la hoja global como último hijo, el link queda al final de `<head>`. |
| Info | `tenant-public-styles.ts:47` | El respaldo solo reconoce estilos con encapsulación emulada (`_ngcontent`/`_nghost`). Un componente con `ViewEncapsulation.None` que se inserte antes quedaría por delante del CSS del negocio. Solo afecta al respaldo, que el build ya vigila. | No hace falta cambiar código. Basta con declararlo. |
| Info | `package.json:14` | `check:public-builds` no está conectado a ningún hook, así que se ejecuta solo a mano. | Incluirlo explícitamente en la suite final, después de los dos builds. |

## Sin cambios respecto a la revisión anterior

- No me pronuncio sobre la equivalencia visual ni sobre las métricas de M1. Capturas con distinto scroll, otro estado del carrito o en plena transición no sirven como prueba.
- D1 (`Cache-Control: no-cache` para `/tenant-public.css`) sigue pendiente antes de cualquier despliegue y necesita aprobación del usuario.
- F4 sigue abierta hasta tener la suite final, `check:public-builds`, el CSS de application idéntico byte a byte y las métricas de M1 medidas.
