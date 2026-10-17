# Verificación del cierre de 007-1

Las correcciones pedidas están en el código, dentro del alcance de 007-1, y no encontré nada que bloquee. La entrega todavía no se puede cerrar: faltan las cifras definitivas de las suites y la revisión visual. Solo leí el código (Read/Grep) y no ejecuté pruebas. No puedo escribir archivos, así que Codex tendría que guardar esto en `revisiones/007-1-cierre.md`.

## Resultado por punto

| Punto | Estado | Evidencia |
|---|---|---|
| **M1** | Cumple | `CitaEntidad.java:19-20` marca las dos columnas `updatable=false`. `CitaRepositorio.java:15-18` hace un UPDATE nativo con empresa, id e `IS NULL`. La entidad en memoria solo se actualiza si `escritas == 1`; si no, deja un aviso en el log (`ServicioAceptacionPoliticaCheckout.java:69-75`). La prueba MySQL `MigracionesPoliticaMysqlTest.java:82-93` carga la cita, acredita, la guarda después y la aceptación se conserva. |
| **M2** | Cumple | `ServicioEstadoPagoCheckout.java:58-60` sigue el orden existencias → aceptación → confirmación, y la aceptación se acredita aunque no haya stock. Hay una prueba InOrder (`ServicioEstadoPagoCheckoutTest.java:42-43`) y otra para el caso sin stock (`:66-71`). El orden real de bloqueos en MySQL sigue en B6, como seguimiento. |
| **M3(a)** | Cumple | `buscarCitasConPagoAnterior` (`CitaRepositorio.java:20-25`) busca pagos directos mayores que cero y ventas PAGADA, sin contar la orden actual. Las citas con pago anterior se quedan sin aceptación (`:68`) y la aceptación queda en la orden. Lo cubre la prueba `:76`. |
| **M4** | Cumple la parte de código | La prueba con Flyway real (`:94-103`) mete la huella alterada justo antes de la guarda de huellas (`V138:40`), después del UPDATE de folios. Comprueba que Flyway falla, que no queda ningún folio guardado y que la historia registra `success=false`. Al final borra la base. La parte de los ajustes de Cloud SQL (GTID, `binlog_format=ROW`) y `flyway repair` pasa al ensayo A1. |
| **M5** | Cumple | `checkout.component.ts:454,459-467` vacía la orden y el pago, genera claves nuevas, desmarca la casilla y recarga la configuración. Los pagos alternativos son solo de productos (`ServicioPagoCheckout.java:143`), así que no necesitan este reinicio. |
| **B1** | Cumple | `validarReintento` decide por las líneas SERVICIO reales de la orden (`:38,82-85`). Prueba en `:85`. |
| **B2** | Cumple | El enlace a la política solo aparece si el sitio está publicado y hay una versión vigente (`ServicioCorreoCitas.java:70`; `ServicioCorreoEventosCita.java:112,117,212,238`). El folio se busca filtrando por empresa (`ServicioCorreoCitas.java:68-69`). |
| **B4** | Cumple | `saveAndFlush` convierte el fallo de unicidad en 409 y no reintenta (`ServicioPoliticaEmpresa.java:96-99`). |
| **B7** | Cumple | Responde 409 con el mensaje «Configura primero el sitio público…» (`:53`). El editor lo muestra desde `e.error.message` (`admin-policy.component.ts:26`). |
| **B3, plural** | **Parcial** | Solo se pluraliza «vez/veces» (`:128`). «Días» no cambia: con valor 1, el texto diría «1 días naturales» (`:126-127` y `:133`). No poner topes de negocio es razonable, porque eran una sugerencia y no una decisión. |

## Observaciones (bajas, no bloquean)

1. **B3:** pluralizar «día/días» en los dos plazos y añadir un caso de prueba con valor 1. Para esto no hace falta una decisión del negocio.
2. **M3, orden de la consulta:** `buscarCitasConPagoAnterior` (`:64`) se ejecuta antes de `bloquearGrupoOrden` (`:67`). Un pago en Caja que entre justo en medio no se vería. Conviene hacer la consulta después del bloqueo. Además, esa consulta solo se ha probado con mocks: falta un caso MySQL (cita con anticipo anterior y otra cuyo único pago es la orden actual), que puede ir en B6.
3. **M5, prueba:** también debería comprobar que cambió `paymentKey`. En la revisión visual hay que confirmar que desmontar el Brick dentro de su propio callback no deja errores en la consola y que la casilla vuelve a funcionar.
4. **B7, pendiente visual:** ver que en el editor aparece el mensaje del backend y no el genérico.

## Alcance

No vi lógica de custodia, crédito ni reagendado (fases 2, 3 y 4). El texto de la plantilla sigue prometiendo esas funciones, pero el despliegue está bloqueado hasta 007-2/3 y A1, y no hay publicación automática. Eso es coherente con A1. Las plantillas de WhatsApp quedan pendientes de configuración antes de 007-3.

## Qué falta para cerrar 007-1

- Total definitivo del backend contado en los XML, incluida la repetición después de los cambios de correo.
- Resultado de la suite Angular con la prueba del 409.
- Revisión visual del editor, la página pública, la casilla del checkout y el flujo de 409, sin publicar la política ni cobrar.

Cuando esas cifras estén en `respuestas/007.md` sin fallos, doy las correcciones de 007-1 por aprobadas. El commit lo autoriza el usuario.
