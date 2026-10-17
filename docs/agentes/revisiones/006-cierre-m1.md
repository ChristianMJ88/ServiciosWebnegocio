Ya revisé M1, B1, B2, M2 y B3; esto es el dictamen.

# Dictamen 006 fase 1: no está lista para commit por un bloqueo en una prueba de M1

**Cómo la hice:** solo con Read, Glob y Grep. No ejecuté ninguna suite ni certifico pruebas. No escribí ninguna revisión. Si quieres conservar el dictamen, conviene guardarlo en un archivo nuevo, por ejemplo `revisiones/006-cierre-m1.md`, sin tocar `006.md` ni `006-cierre.md`.

## Bloqueo

### M1-a — Alta: la prueba del resolutor fallará
- **Dónde:** `ServicioResolucionIncidenciasCheckoutTest.java:78`.
- **Problema:** dice `secuencia.verify(citas).confirmar(7L,30L);`. Ahí `citas` es un mock, y el resolutor ahora llama a la versión de 4 argumentos (`ServicioResolucionIncidenciasCheckout.java:87`). En un mock, la versión de 2 argumentos no se ejecuta, así que Mockito dará "Wanted but not invoked".
- **Diferencia con lo que dice la solicitud:** la solicitud afirma que esta prueba "verifica la llamada con actor 9". No es así: solo se fija la respuesta de la llamada con `ADMIN, 9L` en la línea 75, y la verificación sigue siendo la de 2 argumentos.
- **Corrección:** cambiar la línea 78 por `secuencia.verify(citas).confirmar(7L,30L,OrigenEventoCita.ADMIN,9L);`. Opcionalmente, añadir `verify(citas,never()).confirmar(any(),any());`.
- **Validación:** volver a ejecutar esa clase de prueba, con sus dos parámetros (`LIBERADA_SIN_PAGO` y `PENDIENTE`).

## Correcto
- **M1, código:**
  - La versión de 2 argumentos delega con `WEBHOOK, null` (`ServicioConfirmacionCitasCheckout.java:46-48`).
  - La versión de 4 argumentos pasa origen y actor a `CitaConfirmada` (`:67`).
  - `avisarIncidencia` y `liberar` siguen con `WEBHOOK`/`SISTEMA`.
  - `ServicioEstadoPagoCheckout.java:56` no cambia.
- **M1, prueba del confirmador:** `ServicioConfirmacionCitasCheckoutTest.java:67-75` comprueba dos `CitaConfirmada` con `ADMIN`/9, `verifyNoMoreInteractions(bus)` y dos de `programarConfirmada` y de `programarNuevaCitaNegocio`. La prueba del webhook (`:57-66`) sigue comprobando `WEBHOOK` y actor nulo.
- **B1:**
  - El historial ahora usa `actorId`, con un motivo propio para `RECEPCION` (`ServicioCitasCliente.java:208-210`).
  - La prueba parametrizada (`EventosConfirmacionReprogramacionTest.java:100-119`) comprueba usuario 11 con "recepción" y usuario 20 con "cliente".
- **B2:**
  - `EventosCitaArquitecturaTest.java:31-44` recorre todo `src/main/java` y limita esos dos tipos a 5 archivos permitidos.
  - Con grep salen exactamente esos 5 archivos, así que hoy la prueba pasaría.
- **M2:**
  - La ruta de Staff para confirmar sigue sin publicar nada (`ServicioAgendaStaff.java:143-154`).
  - La prueba de regresión (`CierreCitaUnificadoTest.java:76-83`) comprueba el historial con actor 11 y que no haya llamadas al observador, al correo ni a WhatsApp.
  - Los cierres de Staff siguen pasando por `cierres.cerrar(...STAFF)` (`:126`).
  - Queda como hueco conocido, documentado en la respuesta.
- **B3:** el Javadoc de `OrigenEventoCita.java:3,6` deja claro que es el origen de la operación y no un rol: el cobro de Caja cuenta como `ADMIN`, y en `WEBHOOK` quien inició el cobro en Point se sigue viendo en la venta enlazada. El enum no se amplía.

## Sin verificar
- **B4:** no puedo comprobar con `git show HEAD` que el código anterior también consultaba dentro del `for`, porque no tengo Bash. Lo acepto por lo que declara Codex, pero no lo certifico. Es de prioridad baja y no bloquea.
- Que las suites completas estén en verde.

## Condición para hacer el commit
1. Corregir la línea 78 del resolutor.
2. Tener las suites completas en verde, con las aserciones previas sin cambios.
3. Dejar fuera del commit `revisiones/005.md` y `revisiones/007.md`.

No vi nada de 007, polling, SSE, migraciones ni cambios de API en los archivos que revisé.
