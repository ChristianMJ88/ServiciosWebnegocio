# Cierre final de 007-1 — Claude

Las cifras ya están verificadas. Conté los XML de surefire: 141 suites y 519 pruebas, con 0 fallos, 0 errores y 1 omitida (envío real). Esa ejecución es posterior al último cambio del backend (01:29:35) e incluye `MigracionesPoliticaMysqlTest` con skipped=0. Angular pasó 173/62 a las 04:00, después de los cambios de las 03:56. `git diff --check` sale limpio. Las correcciones de 007-1 quedan aprobadas.

**El usuario pide corregir ahora, antes del commit, las cuatro observaciones bajas que siguen abiertas.** Todas están dentro del alcance de 007-1. Ninguna necesita migraciones ni toca las fases 2, 3 o 4.

## C1 — Plural de «día/días» (B3) · Prioridad media

`ServicioPoliticaEmpresa.java:126-127` y `:133-134` siempre escriben «días naturales». Hay que crear un método privado, por ejemplo `dias(int n)`, que devuelva «1 día natural» o «n días naturales», y usarlo en los dos plazos. «vez/veces» (`:128`) se queda como está.

- Este cambio altera el texto generado y, con él, su huella. Las versiones ya publicadas conservan su texto guardado (las columnas son inmutables), así que no hay efecto retroactivo. En local hay 0 políticas publicadas.
- **Prueba:** en `ServicioPoliticaEmpresaTest`, vista previa con `diasReagendar=1` y `diasAclaracion=1`. Debe contener «1 día natural» y no «1 días». Hay que conservar el caso actual de «45 días»/«100 días» (`:55`).

## C2 — Pagos anteriores consultados antes de bloquear las citas (M3) · Prioridad media

En `ServicioAceptacionPoliticaCheckout.java:64`, `buscarCitasConPagoAnterior` se ejecuta antes de `bloquearGrupoOrden` (`:67`). **No basta con cambiar el orden de las líneas.** Con REPEATABLE READ, el SELECT normal reutiliza la instantánea que ya abrió la lectura de `detalles` (`:60`), así que seguiría sin ver un pago confirmado justo antes.

- Hay que mover la consulta para que se ejecute después de `bloquearGrupoOrden`.
- Hay que confirmar si el registro de pagos directos en Caja bloquea la fila de `cita`, con `FOR UPDATE` sobre la misma cita, antes de insertar `pago_cita`.
  - **Si la bloquea**, la consulta posterior al bloqueo tiene que ser una lectura con bloqueo (`FOR SHARE`, en una consulta nativa) para leer el estado confirmado actual y no la instantánea. Otra opción es documentar y probar que el bloqueo de la cita serializa ambos caminos.
  - **Si no la bloquea**, hay que registrarlo como hallazgo y no corregirlo aquí, porque cambiaría el camino de cobro de Caja. Eso pasa a 007-2.
- **Prueba MySQL** (base efímera, como en `MigracionesPoliticaMysqlTest`): una cita con anticipo directo previo y otra cuyo único pago es la orden actual. Resultado esperado: la primera queda en NULL y la segunda recibe la aceptación. La prueba InOrder debe comprobar que el bloqueo va antes de la consulta.

## C3 — `paymentKey` y recarga fallida tras el 409 (M5) · Prioridad baja

- En `checkout.component.spec.ts:62-69`, añadir `expect((c as any).paymentKey).not.toBe(<clave anterior>)`.
- En `checkout.component.ts:466`, si falla la recarga, `config` queda en `null` y el checkout se desactiva sin explicación. Hay que mantener ese comportamiento seguro y mostrar un mensaje que indique que la política cambió y que hay que recargar la página para aceptar la versión vigente. Añadir una prueba con `config` que falle.

## C4 — Mensaje del backend en el editor (B7) · Prioridad baja

No hace falta cambiar código. Basta una prueba de componente en la que `admin()` o `publish()` respondan 409 con `error.message` y se compruebe que `error()` muestra ese texto y no el genérico. Si se hace revisión visual, que sea sin publicar.

## Validación para cerrar

Suite completa del backend contada en los XML (incluida la prueba MySQL de C2) y suite completa de Angular, ambas sin fallos, más `git diff --check`. Registrar en `respuestas/007.md` qué se corrigió en C1–C4 y, en C2, qué camino se tomó y con qué evidencia. No hay publicación, cobros, envíos ni commit sin autorización del usuario. El despliegue sigue bloqueado hasta 007-2/3 y A1.
