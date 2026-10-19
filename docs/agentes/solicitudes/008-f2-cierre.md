# 008 F2 — correcciones de la revisión

Solo lectura. Respondo a008-f2:
- P2-1: active empieza vacío, sin aria-current falso; scrollspy se implementa enF4 segúnplan.
- P2-2: token --fl-nav-height compartido enhostfluora-marketing, mobile60px+safeAreaTop; host/topbar/anclas usanesevalor. Rootinicio también recibe scrollMargin.
- P2-3/P3-4: CTA superior tiene texto corto Empezar/Probar, minwidth120; aria-label comienza contexto visible y conservaelplancompleto. Prueba de plan sinprueba con nombre80caracteres, sin texto largo enbarra.
- P3-5: retiradaCSS deCTAvertical ypadding85; hero móvil40px trasnuevoNav. SinCTAduplicado.
- P3-6: helper hasMarketingTabs/marketingPath compartido porApp/Nav, normaliza slashfinalydata deruta.
- P3-7: footer con safeInsets para TODASlasrutasmarketing; headerdesktop/tabletusa left/rightmaxsafeAreaindividual, móvilpaddingindividual. Main soloMarketingcompensaamboslados.
- P3-8: oferta permite reintento traserror; spec.
- P3-9: scrollbar-gutter estable temporal mientrasviveNav, restaurado aldestruir; Escape dedetailsdevuelvefocoasummary. Modal mantieneoverflowprevio.

UI real detectóqueShiftTab podía salir deldialogenWebView; se añadiócicladoexplícito deTab/ShiftTab primer/últimocontrol. Pruebaunitarianueva yrevisiónreal: Cerrar→ShiftTab→Iniciar sesión dentrodialog; Tab→Cerrar; Escape→Más, bodyoverflowrestaurado. Menú375×812, alto772/top40, scrollinterno. Captura f2-menu-movil.png. Layoutdeescritorio/tableta ybreakpoints aúnencurso.

Suiteprevia224/73verde, producciónmarketingexit0; suite finalencurso con3pruebas nuevas. UItenant375×812confirmó headeroriginal, sinNavmarketing/nisoporteglobal, theme-colornull yviewportoriginalsincover, Interoriginal; captura tenant-servicios-f2-movil.png. No dominio propio configurado; speccubrehostpropio/contextonull.

Revisa correcciones/alcance. Sin appwrites, backend/migración/commit/despliegue.
