# 008 F0 — coordinación de V140 y contrato

Solo lectura. Usuario ordenó commit010 → MySQL restringido → Postman →008(F0,F1–F4). Commit08ff998hecho, O-H1cerrado por revisión, Postman99casos/197HTTP/206asercionesenverde. Refuerzo final M4 de causasderechazo aún en ejecución. No se empieza F0aplicación antesdecerrarlalíneabase.

Leídos008.md y008-decisiones.mdcompletos. gitfetchallpruneexitoso,9refsinspeccionadas,sinV140niarchivoactual. **V140reservada enESTADOantesdecrear**. Confirmar coordinación y diseño:

- Tabla marketing_soporte_plataforma, singleton global sinempresa_id, PKconfigestable, @Version, correo,telefonoE164,diasISO(1–7)estructuradosJSONo tabladetalle,scheduleLocalTime,zonaIANA,visible,actualizadoEnUTC,actualizadoPorFKplataforma. Semillaeditable con valores delusuario soloenSQL, nada enJava/HTML.
- GET/PUT /api/v1/plataforma/marketing/soporte conpermisoexistentePLATAFORMA_MARKETING_GESTIONAR, HttpSecurityyaexigeambitoplataforma. DTOadminincluyeversion y privadoauditoría. Validacionescorreo lowercase,E164explícito(sin asumircódigopaís),≥1día,fin>inicio,ZoneId,version vieja409. Auditoríaanterior/nuevodentrode mismaTX viaServicioAuditoriaPlataforma.
- APIpúblicaGET /api/v1/publico/plataforma/soporte cache60s, DTO solocorreo/telefono/dias/horas/zona;404sioculto/ausente,sinfallback.
- EditorstandaloneSoporteenplatform-marketing(vista formularioguiadoyversión); no mezclarreglasprivadascon home. Mensajedeerrorbackend. Footer yFAQsoloFluora consumenservicio compartidoyformateadordedías/horariosporIntl;tenantintacto, sincontactosglobales enminisitios.
- Teléfonoeditorrequierecódigointernacional;se normalizaquitandoespacios/paréntesis/guiones yvalidando+[1-9]7–15dígitos. No asumirMéxicoenJava; semillaaprobadaE164. MostrarconfiguradoenUIy telE164;formato nacionalpuede derivarsedelvalor,cuidarsinreglapaísincrustada.
- PruebasWebMvc+servicio:RBAC200/403,validación,409,auditoría,noexposicióndecamposprivados,oculto. FEeditor/envío/errores,footercondatos/ausente,regresióntenant,formatodíasnoL-V. SuitestotalesalfinalF0. MigraciónprimeroenDBefímeraconusuariofluora_tests, nuncaeditaraplicadas.

No escribirapp,migración,ESTADO ni credenciales. Noexpansiónalcanse; sincommit008/producción/despliegue.
