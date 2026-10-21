# R3: implementación del ajuste de anclas y correcciones del harness

Claude, revisar solo en lectura. No ejecutar suites ni leer .env/postman/credenciales. Codex es único escritor de app. Leer PROTOCOLO/ESTADO, r3-anclas y r3-cierre-parcial, y diff actual de Home/Planes/specs y scripts de performance. No commit/deploy ni nuevos cambios de negocio.

## Anclas

- HomePlans: firstRailMeasured falso al cambiar rail; callbackRO escribe canScroll y measured sin await. layoutReady computed considera loading/error/vacío, sin precio o regla propia.
- Home: viewChild tipado de Plans y afterRenderEffect para esperar layoutReady. **También pendingAnchor es signal**, porque el efecto con guardia temprana `if(!pendingAnchor)return` no observaría ningún otro signal en la carga sin fragmento; luego las navegaciones con un fragmento nuevo no lo despertarían. La señal permite los dos estados sin repetir scroll ni forzar detectChanges. Cancelación por gestos y limpieza conservadas.
- 22/22 focales verdes (`/private/tmp/fluora-m3-r3-anchor-focal-final.log`), Home18+Planes4. Incluye overflow/nooverflow, sinRO, error/vacío, cancelación antesRO, resize sinsegundo scroll yfragmentosposteriores/distintos sin detectChanges. En el callback scroll se exigeDOMcontroles real paraoverflow. No ampliar timeouts/workers.

## Harness

- C1-b/c: ventana1000ms sin Documents antes de conteos; app0/document1 en noacceso, app1/document1 y un devmode enacceso. F1 logoFixture explícito y outputs separados.
- H6: CLS empieza en el primer evento, prueba600→5500ms; recalcular métricas desde rawshifts, no descartar corridas. H7: espera chunks JS y componentes iniciales intersectantes; asserts top±.5/altura>=reserva/CLS<=.01/sinfuentesde sección.
- Nuevo arranque transitorio DevTools HTTP closed socket antes de navegar (`/private/tmp/fluora-m3-r3-layout-font-after.log`), no error app. Espera GETs de discovery con retry limitado5s soloECONNREFUSED/RESET/UND_ERR_SOCKET/timeout yChromevivo, nunca reintenta navegación/assert. Unitario cubre sockettransitorio y403noreintentado. Sin restartChrome silencioso.

## Evidencia y estado

CSS200muestras completo,100antes+100después con20paresidénticosporfase/mismas3APIsha. SourceCSSaprobada enreviewprevia. Despuésinitialmax.0030109036,finalscrollmax.02618186. Archivos saneadosbaseline/after en docs/agentes/evidencias. Críticosnormales montados:1024×1367producto top1332.765625estable,height746.4375→746.4531,initial.0013937;900×1440top1366.15625estable,height707.9531→711.0781,initial.0012750. Se comprobarán otra vez conassertsdirectosenúltimoartefacto.

R3 aún no cerrado. Compilaciónúltima, verificación real de anclas, fuentes/carga/CSRfinal,32normalesdelúltimoJS,5suitesyUI pendientes. Códigoanclanuevo no entra en la ruta raíz sin fragmento; se conservará la huella de la matrizCSS y se repetirá32normalesconmismosdatos enúltimoartefacto para no atribuir evidencia aotroSHA. HistóricoproducciónCSRintacto; Node79verdeprevio a2unitariosnuevos, ahora8infra. No backend/Functions/Firebase/RBAC/datos.
