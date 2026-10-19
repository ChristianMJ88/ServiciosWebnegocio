**Veredicto: F0 queda cerrada en local.** Esto incluye código, seguridad, migración V140 local, suites, Postman y revisión visual del editor. No incluye commit, push, despliegue ni el PUT de la colección remota.

No ejecuté ninguna suite. Las cifras (backend 623:622/1 omitida, 158 suites; Angular 210/70; Postman 106/211/237; guardas 8/8; Flyway 140 con success=1) son las que reporta Codex. Solo puedo leer archivos, así que no guardé esta revisión; puede copiarse como `revisiones/008-f0-final.md`.

## Lo que comprobé en el código
- **M-a cerrada.** `ApiAisladaApplication.java:33-36` ejecuta `MysqlAislado.comprobar` y después `comprobarBase(c.getCatalog())`, ambos antes de `SpringApplication.run`. La expresión es `fluora_test_011_[a-z0-9]+` (`:39`). `ApiAisladaApplicationTest.java:5-6` rechaza `null`, `agenda_db`, `fluora_test_base` y un nombre con inyección, y acepta una copia válida. Cumple lo que propuse en 008-f0-cierre.
- **B3 cerrada para el formato**, según la consulta SQL de Codex: `horaInicio` y `actualizadoEn` se guardan como STRING en el JSON real de auditoría. Esto resuelve la duda entre el `ObjectMapper` de la prueba y el de Spring Boot.
- **Ajuste CSS** (`align-items: start`): solo afecta a `support-editor.component.css`.
- **El cambio de 403 en `ConfiguracionSeguridad`** ya estaba aprobado en 008-f0-http403. No lo volví a revisar.

## Pendientes que no bloquean F0
1. **B-a (baja), sin hacer.** Ni `scripts/test-api.py` ni el runner comprueban que la copia esté en `MAX(version)=139` antes de migrar. Conviene añadirlo antes de la próxima migración (F1 o posterior). Si no, una corrida podría aplicar más de una versión sin avisar.
2. **B-b (baja), sin hacer.** Ninguna petición de soporte ni de contacto legal comprueba `Cache-Control`; la única aserción de caché de la colección es la de políticas (`agenda-web…json:1611`). Siguen pendientes también la repetición con `X-Tenant-Slug`, la comprobación de que el 403 no trae `correoLegal` ni `version`, y quitar la aserción duplicada. Conviene hacerlo antes del PUT remoto, para no publicar la colección dos veces.
3. **B-c (baja, nueva).** Guardar los mismos valores sube la versión (0→1) y deja una fila de auditoría. No es un error, pero cada guardado sin cambios ensucia la auditoría. Propongo devolver 200 sin incrementar ni auditar cuando no hay diferencias, o bien dejar la decisión registrada. No bloquea.
4. **Registro desactualizado:**
   - En `respuestas/008-f0.md:33-35` sigue el título «Pendientes de cierre visual» y el texto «Debe completarse la comparación visual del tenant». Si la comparación del tenant ya se hizo, hay que actualizarlo; si no, debe quedar como límite explícito. La solicitud dice «footer protege tenant», pero no dice que hubiera revisión visual.
   - La respuesta tampoco recoge «versión 1 / 1 auditoría» ni la confirmación del JSON STRING.
   - La primera entrada de `ESTADO.md` todavía dice «Postman real en curso». Codex debe añadir una entrada de cierre.
5. **Siguen abiertos fuera de F0:** B4 (dos `role="status"` en `legal-page.component.html:16,34`) y los valores de respaldo de correo en `application.yml`. Están bien como límites declarados.

## Lo que no autoriza esta revisión
- **PUT de la colección remota:** necesita una revisión mía del diff contra el respaldo, que no he hecho, y conviene incluir B-b antes. Además, publicar en Postman es una acción externa que debe autorizar el usuario.
- **Commit de 011/008, push y despliegue:** no autorizados. Siguen vigentes los bloqueos de despliegue de 007-2/3 y de A1 de 004.

**Siguiente paso:** se puede empezar F1. Antes de su migración, recomiendo resolver B-a y reservar la versión con fetch, como en V140.
