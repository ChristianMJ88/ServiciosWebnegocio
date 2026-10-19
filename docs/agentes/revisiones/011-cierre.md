# Revisión 011, cierre de colecciones, arnés y seguridad

**Veredicto:** el aislamiento y la seguridad quedan cerrados. **Todavía no hay aprobación para el PUT.** Faltan tres cosas:

- el resultado de la corrida 99, que no he visto;
- corregir el problema de M4 que describo abajo;
- comparar con la versión remota.

No hay hallazgos altos.

Solo leí archivos, sin ejecutar nada. Esta sesión no tiene herramienta de escritura. Si quieren conservarla con mi autoría, Codex debe guardar este texto como `docs/agentes/revisiones/011-cierre.md`.

## Corregidos y comprobados en el código

- **A1:** las cuatro peticiones llevan una sola cabecera de tenant, con el slug que corresponde. La prueba estática está en `scripts/test-postman-guards.cjs:39-42`.
- **A2:** `exigirUna` cubre el rol global, el rol de empresa y la sucursal (`ApiAisladaApplication.java:97-103`). El login comprueba el rol y que haya permisos (p. ej. `agenda-web…json:216`). Staff tiene una prueba positiva en `:1194`.
- **M1:** la exención exige nombre, GET y ruta exacta `/actuator/health` en localhost, puertos 8080 o 18080 (`:3505`). La negativa contra producción está en `guards.cjs:36-38`.
- **M2:** el servidor de prueba responde con `agenda_db`, y el script exige un marcador y ninguna petición protegida (`test-postman-guards-cli.cjs:6-17`).
- **M3:** comprobado en el código. El mensaje "ya no es valido" solo sale si la firma es válida, el usuario existe y el token ya no está vigente en la base (`ServicioAutenticacionImpl.java:201-202`). El 401 de `:748-775` demuestra la revocación en el servidor, no la falta de cookie. `:1165` documenta que el JWT de acceso sigue vigente.
- **M5:** el entorno se construye desde una lista permitida (`test-api.py:21`) y `MYSQL_PWD` solo llega al cliente MySQL (`:22`).
  - No existe `application-api-aislado.yml`; el único `import` de `.env` es el de `application-dev.yml:3`.
  - Proxy agujero negro en `:62`. Con Boot 3.4.3 sin `httpclient5`, el `RestClient` usa el HttpClient del JDK, que respeta `http(s).proxyHost`. Es coherente, pero no lo he verificado de forma empírica (ver B6).
- **M6:** caducidad de la autorización de escritura entre 0 y 120 s, `/users/me` para todos los tokens y `test_user` para `APP_USR` (`mercado-pago…json:8181-8195`). La variable está vacía en el repo (`:10349`).
- **B1, B3, B4, B5 y B6** corregidos (`:1841`, `ApiAisladaApplication.java:48-53`, `test-api.py:53`, `:1223-1250`, `:2423` y `:3357`).
- Sustituir la negativa de Recepción por `GET /admin/pagos/configuracion` es correcto: no se cambian permisos para que pase una prueba.

## Prioridad media

**M4 sigue sin cerrar: `ERROR_NEGOCIO` no distingue la causa.** Es el código genérico que se devuelve para cualquier `ResponseStatusException` (`ManejadorGlobalExcepciones.java:78-82`). Fijarlo no discrimina nada, así que las negativas solo prueban el estado HTTP. Dos verdes falsos posibles:

- **"Caja A no vende cita B"** (`:2448`): pasaría con el 409 "No hay una caja abierta" (`ServicioVentasCaja.java:166`) si la carpeta 06 se ejecuta sola o falla "Abrir Caja" en la 05.
- **"Versión vieja"** (`:1984`): pasaría si el rechazo se debe a la falta de aceptación y no a la versión.

Corrección: comprobar `mensaje` (texto exacto o fragmento estable) en `:1396`, `:1905`, `:1984`, `:2448`, `:2487`, `:2527`, `:2567`, `:2607`, `:2686`, `:3002` y `:3378`. No hay que cambiar los estados esperados. Algunos mensajes de referencia:

- `:2607` debe recibir "Usa citaId o citaIds, no ambos" (`RegistrarVentaRequest.java:34`).
- `:2448` debe recibir "no está disponible para este negocio" (`ServicioVentasCaja.java:186`).

Para la venta cruzada, añadir el mismo POST con `cita_ausente` y comparar los mensajes. La línea 186 da el mismo mensaje para una cita ajena y para una inexistente; hay que demostrarlo como ya se hizo en B5. Hacer lo mismo con Point ajeno (`:3357`) frente a Point inexistente (`:1053`).

**M7. La colección MP tiene dos `prerequest` a nivel de colección:** la guarda (`:8176`) y otro vacío (`:8200`), además de un `test` vacío. No sé si la importación o el PUT conservan ambos o se quedan con uno; si se pierde la guarda, quedan abiertas llamadas reales a MP. Antes del PUT, quitar los eventos vacíos. Después, hacer un GET del remoto y confirmar que la guarda es el único `prerequest` de la colección.

## Prioridad baja

- **B1n.** El marcador se pide a `pm.environment.get('base')` (`:3510`), mientras que la URL de la petición se valida por separado. Conviene pedirlo al origen de la propia petición, o exigir que `base` coincida con ese origen.
- **B2n.** La fixture copia todas las filas de `rol` (`ApiAisladaApplication.java:122-123`). El onboarding solo copia ADMIN, RECEPCIONISTA, CAJERO, STAFF y CLIENTE (`ServicioOnboardingEmpresaImpl.java:56,243`). Los permisos efectivos no cambian, pero "mismo catálogo" no es exacto. Filtrar con `codigo IN (…)`.
- **B3n.** El login comprueba que haya permisos, pero no uno concreto. Añadir uno estable por rol, por ejemplo `CAJA_SESION_GESTIONAR` para CAJERO y RECEPCIONISTA.
- **B4n.** El proceso de Postman hereda `SPRING_DATASOURCE_PASSWORD` y `JWT_SECRETO` (`test-api.py:59,79`). Darle solo el entorno base de la línea 21.
- **B5n.** La prueba estática de cabeceras solo cruza `sucursal_*` y `servicio_*`. Se puede ampliar a `staff_`, `cita_` y `venta_`.
- **B6n (opcional).** Al arrancar la instancia aislada, hacer una llamada HTTPS de control con el `RestClient.Builder` de la aplicación y exigir que falle contra 127.0.0.1:9. Así queda probado el "sin red" para Mercado Pago Orders, OAuth y Envía, no solo deducido.
- **B7n.** Los directorios sin seguimiento `postman/` (copias de las skills del plugin, con un `postman/postman` duplicado) y `.postman/` (ID del espacio de trabajo) no son parte de 011. Hay que dejarlos fuera del commit.

## Decisiones del usuario (no corregir en las pruebas)

- **B2:** check-in de una cita futura. La colección acredita el comportamiento actual; la regla sigue sin decidir.
- **RECEPCIONISTA con `CAJA_SESION_GESTIONAR`** en el catálogo: conviene que el usuario confirme que es intencional, sin cambiarlo en 011.

## Antes del PUT

1. Corrida 99 con exit 0, más una nueva corrida después de corregir M4.
2. M7 resuelto.
3. Comparar con el remoto mediante un GET; si hay cambios ajenos, integrarlos o consultar al usuario.
4. Hacer el PUT solo con las versiones del repo, sin entornos, y un GET posterior para confirmar que las guardas siguen ahí.

Sin commit de 011 ni de 008, sin push ni despliegue.
