# R3: revisión de código y criterios antes del cierre de suites

Claude: solo lectura, sin .env, postman/, credenciales ni ejecución. No modifiques aplicación. Leer PROTOCOLO, ESTADO, revisiones R3-a/b/c/d y límites CSR; el usuario aprobó R4 commit y avanzar T2→R3, y aprobó expresamente caption en segunda fila768–1023 tras las capturas820/1000. Sin commitR3, push, hosting ni producción.

## Código actual

- R3-a configuración/gate/lease y R3-b server/probe/guardas, ya revisados, con condiciones atendidas.
- Nuevos scripts performance: browser-probe +tests, probe-page, hydration-probe, csr-hydration-probe, navigation-hydration-probe, font-hydration-probe, home-sections-measure, home-viewports, loading-plans-measure, caption-proposal. Guardas Node nuevas y fixtures malicious renderers; Functions no se modifica.
- CSS solo Home/hero: caption aprobado, min-height96 en chips tablet (medido960/1000=96;1023=44, hueco52 declarado), reservas por mínimo(height−2*space), sin min-height a contenido cargado, height fija ni conteo de planes.
- Evidencia permanente saneada `docs/agentes/evidencias/008-f4-m3-r3d-baseline.json`:100muestras/20tamaños,20pares normales de alturas idénticas, una huella por API. Los mínimos incorporan bordes767/1023/1199/1200, difieren de tu tabla provisional (desktop producto639.328, no653.84).

## Evidencia Codex y límites

1. FontHold antes CSS:10anchos, heading−31.1875en812/820 antesJS, confirmado con respuesta de fuentes. Fuente original del salto, no App/theme. Propuesta aprobada aplicada. Nueva compilación/probes/CSR producción y guardas pasan. Node79/79 verde fuera de sandbox; cincofallosEPERM previos documentados, no errores del código.
2. Primeras3tamañosprod/dev: DOMreal reutilizado y dev8components/401nodes/0skipped.6replays375 normales/lentos pasaron foco+latencia, cifras en respuestaR3. Son anteriores aCSSfinal; repetir selección final.
3. CSR corregido tras falsoverdeaccess: beforeunload binding captura DOM anterior, Document de appOrigin distinta queda pausado, cuenta1→1 y un devmode. Prodactual7casos exit0 (`/private/tmp/fluora-m3-r3-confirmed-csr-prod.log`). Devderivado7 ejecutados, pero directorio también NG0913 porque su DTO llama `imagenUrl` al logo, no `logoUrl`. Última corrección del harness: con --logo null, además del clone SiteConfig.logoUrl=null, intercepta únicamente GET marketplace, copia respuesta real y pone negocios[*].imagenUrl=null; registra publicApiOverrides campo/cantidad, no modifica API ni backend. Probe real sigue dando exit1 si cualquier warning; no filtros. Falta repetir esas dos variantes tras la matriz. Revisar C1/captura/fixtures/honestidad del límite.
4. Postmatriz100 en curso (`/private/tmp/fluora-m3-r3d-after-full/364e57519e39-1792517832841.json`):820 inicial.169→.0020276;375 initial0, final(scroll/defer).01637 residual. **No equiparar CLS inicial al acumulado durante scroll**. Aplicamos el mínimo aprobado, sin congelar altura de cards ni datos configurables. ¿Se interpreta criterio original «CLS de carga≤.01» como initial, manteniendo scroll como informativo según tu revisiónR3-d §3? Si es bloqueante adicional, indicar corrección acotada o decisión necesaria, no ocultarlo.
5. Dos placeholders cruzaban el primer viewport enbaseline: producto en1024×1367(top1332.77),900×1440(top1314.16). Tras JS se activa onviewport real. Registrar como casos críticos según diseño; ¿cuál es el criterio concreto de aceptación? No se ocultan ni difieren artificialmente para una métrica.

## Pendiente, no acreditado todavía

- Matriz posterior100 completa y comparación de mismas huellasAPI/pares/alturas/residuales.
- FontHold después y loadingplanes16; cnav/GSAP/resize actual; CSR devfinal derivado y real con exit1 esperado documentado; UI4200.
- Cinco suites normales finales360aprox/87 (sin cambiarworkers/timeouts), revisión final después de resultados.
- Limpieza: baseline100 ciclos Chrome completados y profiles borrados porclose; al final se revisarán procesos/puertos/prefijo temporal. Primer fallo antiguo dejó gnTOxA conocido; no borrar perfiles ajenos. Sin serie dedicada20 aún; cien ciclos de la matriz más comprobación de residuales deberían acreditar el criterio, confirma si falta una verificación distinta.

Solicito revisión de defectos concretos de código y decisión sobre criterios/residuales. No otorgar aprobación final con pruebas aún pendientes. Tampoco iniciar prerender productivo ni modificar dominio/credenciales/backend/RBAC.
