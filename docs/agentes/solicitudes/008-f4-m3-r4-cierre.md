# 008 F4 M3 R4: revisión de correcciones y cierre

Autor: Codex, 2026-10-20. Revisa exclusivamente en lectura, sin ejecutar comandos/builds ni modificar aplicación o documentos. No leer .env, postman/ ni credenciales. Conserva la autoría de tu revisión previa. Usuario autorizó R1+commit (00ec341) y continuar R4; detenerse antes de R3, sin commit de R4/push/deploy.

Lee `revisiones/008-f4-m3-r4-codigo.md`, la respuesta actual `respuestas/008-f4-m3-r4.md` y las pruebas corregidas:
- `src/app/marketing-server-shell.spec.ts`: M1 control positivo real con browser, mismos targets window/document/Storage.prototype, lectura de sesión + scroll/click/keydown + matchMedia. Servidor espías tóxicos registran llamadas, cero efectos/console.error. B2 assertions en html.
- `core/platform/marketing-render-context.spec.ts`: B1 integrado marketing true/app false/tenant false.
- `core/platform/platform-host.service.spec.ts`: B4 espacios/barras y location/runtime ausentes.
- `core/marketing/marketing-sections.directive.spec.ts`: controles browser existentes IO/RAF/cleanup y server IO tóxico/RAF cero.

El harness jsdom ejecuta @defer del renderer browser: no es SSR real ni ngServerMode. En shell se usa IO no notificante para placeholders del framework y se comprueba que ningún id del scrollspy se observe. Directiva aislada sí envenena IO. No se suprimen errores de consola; se comprueban cero tras whenStable. B3 primer pintado se registra para R3, sin implementar hidratación.

Codex ejecutó dos suites completas normales consecutivas de 341/85, Node20/TZ México, exit0 (42.46s y58.11s), sin stderr/ERROR/Unhandled. Node22 37/37 y ambos builds/guardas/cmp verdes. UI 375x812,820x1180,1440x900,menú/Escape/foco/scrollspy,GSAP desktop,UTM,tenant/acceso y0consola, pestaña propia cerrada/viewport restaurado. Detalles y logs en respuesta.

Último ajuste en verificación: build marketing inicial 477.36kB frente a476.17 (+1.19), fuera del ±1kB. Se retiran cuatro copias redundantes de campos de PlatformHostService: métodos públicos leen directamente snapshot, sin cambiar función pura, validación o clasificación. Rebuild/tamaño y repetición de suites por este último ajuste se documentarán antes de entregar. No atribuir las dos suites anteriores a ese último cambio ni certificar pruebas no ejecutadas por ti.

Confirma si M1/B1/B2/B4 quedan cerrados y si la lectura directa del snapshot conserva el contrato. Señala cualquier bloqueo antes de entregar R4; B3 y SSR real quedan explícitamente para R3. Devuelve la revisión completa para que el script la guarde en revisiones/008-f4-m3-r4-cierre.md.
