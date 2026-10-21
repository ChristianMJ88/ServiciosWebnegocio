# R3-a: revisión de configuración, compuerta y metadata

Autor Codex,2026-10-20. Solo lectura, no ejecutar suites/builds/cambiar código, no .env/postman/credenciales. Usuario aprobóR4commit(e53cc51),T2ycontinuarR3. T2cerrado con5suites341/85normales y revisiónT2aprobada; specsT2 quedan sincommit y no se editan. NoR3commit/push/deploy ni activarprerender productivo.

DiseñoR3etapa-a aplicado:
- app.config.marketing único provideClientHydration(withEventReplay,withNoHttpTransferCache), createMarketingAppConfig(hydrate), localecompartido ymainmarketingselector; base/general/applicationintactos.
- isHydratableMarketingHome usa resolvePlatformHostSnapshot y el predicado existente isMarketingHostname/getHostnameFromOrigin. **No añadí isMarketingSnapshot ni cambié el método de clase**: así no se pierde el hostname cacheado dePlatformHostService ni se duplica una regla. Se reutiliza el predicado puro preexistente (las11caracterizaciones estaban verdes en5suitesprevias).
- Homeflaghydrated conafterNextRender solo browser, heroVerticals/dataReady compuerta; HeroofferReady/fallbacklabelcentral enOffer. No timers/nuevasAPI/precioshardcodeados.
- Appearance serializa origen delease condata-fl-original/data-fl-lease y restaura deatributosSSR, incluyendo originalvacío; server/documentMeta válido.
- 12casosdestino,2casosmetadatasserver,1casoHomeconof()sincrónico+ofertaprecargadaantescrearHome yprimeraestructuraDOMcapturadaenmisma pila antesdewhenStable; servidorR4assertshydrated/dataReadyfalse. Asercionesexistentesconservadas.
- Nuevaguardadebootstrap en scripts/marketing-hydration-wiring.test.mjs:3/3Node22verdes. FocalAngular/buildcompletos/guardas en curso, no atribuirteejecución.

Lee archivos nuevos/modificados de esaetapa, specs yrespuestasinventaralcanceT2. NoestáninstaladosSSR/serveryangular.json/firebaserc/Functionsno se modificaron.

Puntospara confirmar antes deR3-b:
1. La compuerta despuésdeprimerDOMfunciona ante oferta/Navprecargadas, y no rompe anclas; cualquier falsopositivodelspec debe corregirse, no saltarselaespera.
2. RootmarketingCSR enR3-a proveehidratación pero Angular21auto-desactiva si falta __nghData__; NG0505 soloendev sinngcm, prodoptnoaviso. LasrutasCSRdistintasde/ no tienenprovider. ¿Hay que añadirun detector explícito deestadoserializado paraCSRroot ahora, o basta probarnativeCSRngcm enprobe? No ocultarerrores deSSRrotoconfallback aCSR.
3. **Sin orígenes duplicados/hardcodeados nuevos enangular.json**: propongo que unbuildscript deprobe lealosdefaults públicos depublic/runtime-config.js (vmconhostbuild.invalid), valide HTTPS/distintos y pase define aAngularCLI porargsestructurados; angular.jsonsolooptionsserver/outputMode/outputPath. La guarda deprobe compara manifest/configderivado conruntime; builddirectosin define falla cerrado. Unasolafuente deorígenes, sin.env/credenciales. Confirmaestaalternativaa2literalesduplicadosdeorígenes enconfig.
4. ConcreateMarketingAppConfig(false) ninguna hidratación delárboltenant; markerattrsMeta nointerfieren restauracióndeNav/Home concurrentes o metadataoriginalsinattrs.

Devuelve hallazgos yveredictoR3-a; se registraránresultadosCodexantes deR3-b. Noautorizarmásalcancequenousuario. Scriptguardararevisiones/008-f4-m3-r3a-codigo.md.

Adenda de observación del spec: el focal inicial dio42/43; el caso nuevo esperaba0chips al volver de fixture.detectChanges, pero TestBed ya había ejecutado afterNextRender y otra pasada, mostrando1. Se mantiene la misma aserción0/fallback/sintrial, capturando primitivas dentro de ngAfterViewInit (spy que ya evita GSAP) antes del callback afterNextRender. Luego se comprueba el DOM final con API. No se desactiva la compuerta ni se falsean timers/render hooks. Reejecución en curso; no declarar verde antes del log final.
