# 011: pruebas de API con Postman CLI

Autor: Claude Code (arquitectura y revisión), 2026-10-17. El usuario pidió instalar Postman CLI y agregar pruebas. Codex escribe y ejecuta la colección; Claude la revisa.

## Estado del entorno
- Postman CLI está instalado globalmente con npm (`postman-cli`), actualizado de 1.33.1 a **1.75.0**.
- `postman collection run` **pide iniciar sesión**: la sesión guardada caducó (401). El usuario inicia sesión con `postman login` y su API key. Ningún agente pide, guarda ni lee esa clave.
- Plan B si no se usa una cuenta de Postman: `newman` corre la misma colección (formato v2.1) sin iniciar sesión.

### Actualización 2026-10-18
- El usuario ya configuró Postman CLI para Codex y para Claude. Claude comprobó una colección local de humo (solo lectura): 0 fallos.
- **Condición previa:** O-H1 de `revisiones/010-aprobacion.md`. Hay que tener el usuario MySQL de pruebas, limitado a esquemas efímeros, antes de crear la copia en la que escribe la colección.
- Correr siempre **desde el archivo del repo**, nunca por id de colección, para no publicar resultados en la nube de Postman.

### Decisión del usuario 2026-10-18: usar sus colecciones en la nube, con el repo como fuente
El workspace de Postman del usuario ya tiene las colecciones **«agenda web»** y **«mercado pago»**. Las pruebas se construyen sobre ellas y **no** se crea una colección nueva desde cero. Flujo:

1. **Respaldo.** Antes de tocar nada, Codex exporta las dos colecciones y los entornos que usan (Postman API `GET /collections/{uid}`, con la clave en una variable de entorno, por ejemplo `POSTMAN_API_KEY`; nunca se escribe en archivos ni en logs). Guarda una copia **privada** fuera del repo (`/private/tmp/...`), porque puede contener tokens.
2. **Limpieza y versionado.** Se copian al repo, en `backend/postman/agenda-web.postman_collection.json` y `backend/postman/mercado-pago.postman_collection.json`, **sin secretos**:
   - variables con tokens, contraseñas, access tokens de Mercado Pago o cookies quedan como `{{variable}}` vacía;
   - se quitan `auth` con valores literales y respuestas de ejemplo con datos reales (correos, teléfonos, ids de pago reales).
   - Claude revisa el diff de esta limpieza antes del commit.
3. **Extensión.** Las carpetas 00–07 de este documento se agregan a «agenda web», respetando lo que ya existe: no se borran peticiones del usuario y, si una está desactualizada, se marca en `respuestas/011.md`. La regla de entorno aislado aplica igual.
4. **Ejecución:** siempre desde el archivo del repo, para no publicar resultados.
5. **Subida a la nube.** Cuando la corrida está en verde, Codex actualiza las colecciones del usuario (`PUT /collections/{uid}`) con la versión del repo, **sin secretos**. Los valores privados los conserva el usuario en sus entornos de Postman. Antes del PUT, Codex compara con el respaldo del paso 1: si alguien cambió la colección en la nube mientras tanto, no sobrescribe, sino que integra los cambios o pregunta.

**Colección «mercado pago»:** llama directamente a la API de Mercado Pago.
- Solo con credenciales de **PRUEBA**, en variables de entorno.
- Las peticiones de lectura (GET) se pueden correr.
- Las que crean pagos, órdenes, reembolsos o configuran terminales o webhooks se corren **solo con autorización explícita del usuario para esa corrida**, y nunca con credenciales de producción.
- La colección revisa al inicio que el token sea de prueba (prefijo `TEST-` o la cuenta de prueba esperada) y, si no lo es, salta las peticiones que escriben.

## Estructura (en el repo)
- Las colecciones del usuario, exportadas al repo y sin secretos (ver la decisión de arriba): `backend/postman/agenda-web.postman_collection.json` y `backend/postman/mercado-pago.postman_collection.json`, en formato v2.1 y organizadas por carpetas.
- `backend/postman/aislado.postman_environment.example.json`: solo nombres de variables con valores vacíos o de ejemplo. **Ningún secreto se guarda en el repo.** Las credenciales de los usuarios de prueba se pasan con `--env-var` desde variables de entorno del shell.
- `backend/postman/README.md`: cómo levantar la instancia aislada, cómo correr la colección y cómo leer el reporte.
- Script `npm run test:api` (o `scripts/test-api.sh`) que corre la colección con `--reporters cli,junit` y exporta el resultado a `backend/target/postman/`, que no se versiona.

## Regla de seguridad: dónde se puede correr
- **Las carpetas que escriben datos** (reservas, cobros, publicar política, Point) **solo corren contra una instancia aislada**: copia temporal de la base, backend en 18080, tareas programadas y envíos desactivados, Mercado Pago en modo PRUEBA y Point simulado. Es el mismo procedimiento de 007-1 y 009.
- Hay un script en el nivel de la colección: si `entorno !== 'aislado'` o `base` no apunta al puerto aislado, las peticiones que escriben se saltan (`pm.execution.skipRequest()`) y se registra un fallo explícito («Entorno no aislado»). Solo la carpeta `00 Humo (lectura)` puede apuntar al backend habitual en 8080.
- Antes de escribir, la colección consulta en la copia un marcador del entorno aislado, por ejemplo el nombre de la base o un parámetro propio de la copia. Así se evita depender solo del puerto.
- Nada de envíos reales de correo o WhatsApp, ni de cobros reales. Al terminar se borra la copia y se comprueba que el puerto 18080 quedó libre.

## Carpetas y casos

**00 Humo (lectura)**
- health en UP;
- login de cada rol de prueba (admin, recepción, caja, staff) y logout con 204;
- catálogo público, política pública y configuración pública de checkout sin campos privados (sin ids globales, actor, avisos privados ni configuración de pagos).

**01 Seguridad: tenant y RBAC**
- Para cada rol, los endpoints que no le corresponden devuelven 403.
- Ids de otro tenant devuelven 404, siempre la misma respuesta: citas, ventas, órdenes Point y política.
- Slug inexistente, sitio sin publicar y negocio sin política devuelven el mismo 404.
- Sin token devuelve 401. Con token caducado o con refresh, el flujo es el esperado.

**02 Políticas (007-1)**
- Admin consulta la política, pide la vista previa (singular con 1 día) y publica (versión consecutiva).
- Publicar con un `versionBase` viejo → 409.
- Publicar contenido idéntico → se deduplica.
- La página pública muestra la versión vigente y su caché es de 60 s.
- La alerta del administrador aparece cuando hay servicios con anticipo y no hay política.

**03 Reservas y anticipo (009 P1 y 010 punto 3)**
- La cotización indica `reservableEnLinea` en cada caso.
- Reserva pública de un servicio con anticipo **sin política** → 409 `ANTICIPO_NO_DISPONIBLE` y no se crea nada. Se comprueba consultando como admin.
- Con política y checkout de prueba → `PENDIENTE_PAGO` con apartado. Reserva mixta → el importe es la suma de los anticipos.
- Sin anticipo → `PENDIENTE`.
- Recepción (`publico=false`) → se crea igual que antes.
- Bot: solo la guarda del backend (origen BOT → 409). La conversación se prueba en las pruebas unitarias, porque la firma de Twilio no aplica aquí.

**04 Checkout y aceptación (007-1)**
- Crear una orden con servicios sin aceptar la política → 400 o 409.
- Con una versión vieja → 409.
- Una orden solo de productos sin aceptación → 200.
- Reintento idempotente con la misma clave.
- **No** se procesa un pago con tarjeta real. El webhook aprobado solo se prueba si el entorno aislado tiene un simulador; si no lo tiene, queda en las pruebas de integración de Java.

**05 Recepción (009 P2)**
- Crear una cita, hacer check-in, cobrarla y comprobar el cambio de `seccionAgenda`: ACTIVA → FINALIZADA.
- Una cancelada → CANCELADA.
- Una pagada en línea sin check-in → ACTIVA.

**06 Caja (009 P3 y 010)**
- Venta con dos citas del mismo cliente más un producto → una venta con tres líneas y saldo 0.
- Otro cliente, sucursal o moneda → 409 sin cambios.
- `citaId` y `citaIds` a la vez → 400.
- Cita ya pagada → 409.
- Pago parcial directo y después saldo restante.

**07 Point y pago dividido (010, puntos 1 y 4).** Se agregan cuando cada punto esté implementado.
- Point simulado en curso → un pago directo de la misma cita da 409.
- Simular la aprobación → venta `PAGADA`.
- Cancelar → saldo liberado.
- Point con varias citas.
- 700 = 300 en efectivo + Point por 400 → la orden lleva 400 y, al aprobarse, quedan dos pagos.
- Rechazo → 0 pagos registrados.
- El corte suma cada parte en su método.

## Cómo se escriben los casos
- Cada petición tiene pruebas de **estado, forma del contrato y regla de negocio**, no solo del código de estado. Ejemplo: en el 409 se comprueba también el `codigo` estable y que no se creó el recurso.
- Los ids generados se encadenan con variables de colección. El login guarda el token en una variable de colección y nunca en el entorno exportado.
- Los datos se crean dentro de la propia corrida, en la copia. No dependen de ids fijos de la base habitual.
- Fechas en UTC con `Z`, igual que el contrato de 004.

## Orden de trabajo
1. Primero el commit de 007-1 y 009 (`revisiones/010.md` §0).
2. Montar la estructura y las carpetas 00–06, que cubren lo que ya está aprobado. Así queda una línea base de regresión **antes** de 010.
3. Agregar la carpeta 07 a medida que se implementen los puntos 1 y 4 de 010, y actualizar 03 con el punto 3 (bot).
4. Registrar en `respuestas/011.md` la corrida con el reporte junit: peticiones, aserciones, fallos y omitidas, más la confirmación de que la copia se borró. Un fallo de la colección se trata como cualquier otra regresión: no se ajustan las aserciones para que pasen.

## No incluido
- CI o ejecución remota: no se publica la colección en la nube de Postman sin una decisión del usuario.
- Pruebas de carga.
- Pruebas contra producción.
