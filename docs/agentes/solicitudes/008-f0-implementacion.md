# 008 F0 — revisión de implementación

Solo lectura. Revisa cambios F0 respecto a 008-f0-diseno y adenda008-f0-legal. V140 coordinada trasfetch actualizado9refs sincolisiones; creada pero NO aplicada al negocio habitual. Se ensaya en BD efímera restringida. Contacto legal separado confirmado por usuario.

Archivos: módulo marketing nuevos ContactoPlataformaEntidad/repositorio, DTOs SoportePlataformaSolicitud/Response/Publico/ContactoLegal, ServicioSoportePlataforma/Impl, controladores SoportePlataforma/ContactoPublico; V140. Front core/marketing/platform-contact.service y support-format; SupportEditor en platform-marketing, Footer, FAQhome, LegalPage; specs nuevas.

Control optimista y errores409 antesauditoría, sin reglasfront ni tenantId. SoloPLATAFORMA_MARKETING_GESTIONAR existente y seguridad de ámbito existente. DTOpúblicosinactor/version/legal, legalindependientevisible. Correoslimitadosparamailto, zonaIANA, teléfonoE164sinpaísasumido, horas/días. AuditoríaUTC. Footerespera ruta/host ytenant para solicitarsoporte, norespaldo.

Compilaciones backend/Angular correctas. Suites completas en curso: realesMySQL V140/validate/carreratransaccional200+409/una auditoría; unitvalidaciones/RBAC/DTO/cache/ausente; FEfooter/hostyroutecontenantnull/editor/legal/días. Revisa también límites/nuevaspruebas. No atribuir suiteverdehastaevidencia.

No modificar archivos ni app. Sin commit008 ni despliegue. application.yml defaults de salida quedan residuales fueraF0, según adenda. 011cambiospreviosnoatribuirF0.
