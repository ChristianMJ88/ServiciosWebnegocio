# T2: revisión del cambio de pruebas

Autor Codex, 2026-10-20. Solo lectura, sin ejecutar suites/builds ni modificar archivos. No .env/postman/credenciales. Usuario autorizó checkpointR4e53cc51 y correcciónT2antesR3. R3 sigue sin implementar ni instalar paquetes; diseñoR3 recibido por separado.

Lee diseñoT2 y respuestaT2 completos, y los tres specs actuales. Se siguió P1/P2: setup fresco por caso en beforeEach, sin beforeAll/stubs/NO_ERRORS_SCHEMA/cambio de configuración; whenStable y controles observados añadidos. Todas las aserciones anteriores continúan. Promociones usa overrideProvider, mantiene es-MX y agrega asserts locale/llamada. Caja mantiene renderObject.assign sin setInput, contratos separados y contexto sinpromoción explícito.

D0:9 ejecuciones aisladas verdes antes de cambiar cadaarchivo, JSON `/private/tmp/fluora-m3-t2-d0.json`; todos los totales de archivo<1s, casos rápidos<300ms solo con cota delreporter, no inventados. Sondeo de faseBooking:compile1225/DOMacumulado6753/selección7295ms; retirado. Primera reproducción338/341 y segundo sondeo335/341 por6timeouts (incluyeApp/Nav), registrados sinomitirlos. No atribuir el éxito exclusivamente a whenStable.

Declaración importante en respuesta: movermontajeahook NOloacelera, usa hookTimeoutpredeterminado10s vs testTimeout5s. No se aumentaronvalores. El reporter incluye hooks; cualquiercaso>4s se publica como residual. Mejora real de coste: quitarJIToverrideComponentPromociones. Focalcorrecto10/10en3archivos. **Cinconormalesfinalescompletas341/85todasexit0**:36.33s/44.41s/44.95s/41.76s/36.68s. Corrida4BookingShellerror:false4214ms,únicoresidual>4s. JSON `/private/tmp/fluora-m3-t2-series-1.json`, tablaenrespuesta. No certificar pruebasque noejecutaste.

Revisa pérdida de cobertura, mocks/tipos, aislamiento, aserciones y exactitud de declaración de hookbudget. Si hayalgo que cambiar antes de acabarserieo pasarR3, informa prioridad/archivo/caso. Siaprobado, condición es cinconormales completas consecutivasverdes y evidenciaactualizada, sincommitR3/push/deploy. Devuelve revisióncompleta paraque scriptguarde revisiones/008-f4-m3-t2-codigo.md.
