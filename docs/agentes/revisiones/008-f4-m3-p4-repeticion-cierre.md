**Dictamen: P4 repetida cumple la regla en preview local. Puede registrarse el cierre.** No hay bloqueantes. Hay dos observaciones de entorno que conviene registrar con precisión y dos ajustes de evidencia, todos de baja prioridad. No hay autorización para commit, despliegue ni creación de sitios, y N1/N2 de 012 siguen siendo previos a cualquier despliegue.

Revisé solo en lectura. No ejecuté ni modifiqué nada y no leí `.env`, credenciales ni HTML o datos privados de la API.

## Lo verificado

- **Serie y orden:** en `run.log:3-14` los warmups son las secuencias 1 y 2. Las secuencias 3 a 12 siguen el orden AB/BA/AB/BA/AB y suman 5+5 medidas. El orden de modificación de los JSON del directorio coincide.
- **Huellas:** `run.log:1-2` muestra `86a0c011…` para CSR y `da5fbb28…` para P. Las cinco corridas P tienen recursos idénticos: 44 peticiones y 424 253 B. Eso es coherente con que se sirvió el mismo artefacto en todas.
- **Medianas:** las recalculé.
  - P ordenado da 75/81/85/87/88, así que la mediana es **85**.
  - CSR da 82/82/82/83/83, mediana **82**.
  - Las medianas por métrica (LCP 3693.17, FCP 2943, TBT 122, CLS .00977409) también cuadran.
  - El 75 se conserva.
- **Criterios por corrida de P:** A, BP y SEO dan 100 en las 5. CLS ≤ .01 en las 5. `headerRequested:false` en las 5. Las 10 corridas cumplen también el CLS.
- **Extracción del LCP:** en `p-prerender-4.json:5398-5403`, `lcp-discovery-insight` aparece como `notApplicable`, con `score:null` y sin `details`. Como es un objeto existente, un `||` lo elegía y por eso salía null. El selector `…div.fl-home-hero-copy > h1#home-title` está dentro de `lcp-breakdown-insight` en los 5 informes (líneas 5562, 5591, 5551, 5381 y 5525). La corrección del extractor temporal es válida y no requiere volver a medir.
- **No se heredan cifras anteriores:** `p4Repeat` es un bloque aparte, con `inheritsPriorScores:false`. Los bloques `baseline` (71), `prerender`, `userDecision` («Volver marketing a CSR») y `csrRevert` siguen intactos en `008-f4-m3-p-resumen.json:11,115,685-853`.
- **Alimentación:** AC Power antes y después, `lowpowermode 0` en AC y 1 en batería, sin cambios. Sin avisos térmicos. Ninguna afirmación causal.
- **Frontend:** 82535 pasó a `T` durante la serie y volvió a `R` (`frontend-after.txt`).

## Hallazgos

| # | Prio. | Evidencia | Hallazgo | Acción |
|---|---|---|---|---|
| H1 | Media | `processes-measurement-before.txt:700,709` | Al empezar había dos procesos `claude` vivos: PID 8563 y 10925, hijos de shells de Codex, con 0.1 % y 0.0 % de CPU. No parecen revisiones activas, pero «sin Claude durante la serie» es más fuerte que lo que muestra la evidencia. | Redactarlo así: «dos procesos CLI de Claude inactivos presentes; ninguna revisión en curso». No invalida la serie. |
| H2 | Media | mismo archivo, `:629`, `:63`, `:58`, `:314` | Había carga de fondo: un `mysqld` de Homebrew (PID 42960, 43.1 % CPU), aparte de `agenda-mysql` en Docker, además de `syspolicyd` 69.5 %, `mds_stores` 21 % y `mediaanalysisd` 20 %. La alternancia AB/BA reparte esa carga entre los dos artefactos. | Anotarlo como observación, sin atribuirle causa ni usarlo para repetir o descartar corridas. |
| H3 | Baja | `008-f4-m3-p4-repeticion-resumen.json:259`; el directorio crudo solo tiene las huellas iniciales | `artifactsUnchanged:true` no tiene detrás un hash posterior a la serie guardado. | Si Codex calculó el hash posterior, persistirlo (`fingerprintAfter`). Si no, decir «verificado en consola, no persistido». No recalcularlo ahora sobre un `dist` que pudo cambiar. |
| H4 | Baja | `008-f4-m3-p-resumen.json:2-9` | El `status` de nivel superior ya describe la repetición, pero `method.powerObservation` («battery 9 %, discharging») corresponde a la serie original. | Aclarar que `method` se refiere a la serie original, o remitir a `p4Repeat`. Sin borrar nada. |

## Riesgos que deben quedar escritos

- **Mediana justo en el umbral:** P varía 13 puntos (75–88) y CSR solo 1 (82–83). Con la corrida 2 un punto más baja, el resultado sería 84. El cumplimiento es válido, pero no tiene holgura.
- **CLS determinista y cercano a .01:** 4 de 5 corridas dan exactamente .00977409, a .000226 del umbral. La corrida de 75 tiene CLS 0 y un Speed Index de 4761 ms, mayor que su LCP: su patrón de pintado es distinto, aunque el nodo LCP sigue siendo el h1. Cualquier cambio futuro en el hero o en las fuentes podría cruzar .01. Esto es para vigilar, no para ajustar ahora.
- **Alcance:** el resultado acredita solo el preview local en 4173/4174. No dice nada sobre Hosting real.

## Lo que no verifiqué

- El backend actual (PID 49912, salud 200) ni las comprobaciones públicas en curso.
- Las suites Angular 3×387/92 ni Node 190/190: las tomo de la respuesta de Codex.
- Los archivos `power-*.txt` en crudo: solo vi su transcripción en el JSON.
