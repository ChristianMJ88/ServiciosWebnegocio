# Atribución y medición — fase 4

`MarketingTrackingService` es el único punto de conservación y emisión. Las landing pages y el registro real lo reutilizan; no modifica precios, planes, módulos, trial ni pagos.

## Implementado

- Captura `utm_source`, `utm_medium`, `utm_campaign`, `utm_content`, `utm_term` y la vertical de la landing publicada.
- Acepta únicamente identificadores de campaña de hasta 80 caracteres: letras ASCII, números, guion y guion bajo; rechaza direcciones de correo, espacios, URLs y secuencias largas de números. Los identificadores de campaña deben definirse sin información personal; esta validación no sustituye esa responsabilidad.
- Conserva el contexto más reciente en `sessionStorage` y transporta únicamente esos parámetros en enlaces al registro, incluyendo cambios entre los orígenes de marketing y aplicación. Mantiene el identificador real del plan.
- El registro recupera el contexto antes de comenzar el acceso social, por lo que permanece en la sesión del mismo origen al regresar del proveedor.
- Hooks para `landing_view`, `signup_click`, `signup_started`, `signup_completed` y `business_created`. Los dos últimos ocurren después de una respuesta exitosa del endpoint real de onboarding.
- Eventos limitados a nombre, vertical, source, medium y campaign. No incluyen formulario, correo, teléfono, contraseña, tokens, URL completa, contenido ni término de búsqueda.
- Comprobado en navegador: los tres CTA de registro conservaron la campaña; `Elegir Profesional` abrió el formulario existente con `plan=2`, los UTM y `landing_vertical=unas`. No se creó una cuenta artificial para probar ese recorrido.

## Preparación, todavía sin medición externa activa

No se instaló un proveedor ni se configuraron IDs reales. `__FLUORA_MARKETING_TRACKING__.enabled` tiene valor inicial `false`. El adaptador `dataLayer` necesita además consentimiento explícito mediante `setConsent(true)`; falta conectar la interfaz de consentimiento y configurar 1|2activar la medición. No se conservan ni reenvían eventos anteriores al consentimiento.

Al configurar etiquetas no habilitar vistas automáticas que copien URL completa o query strings: el acceso social utiliza tokens en los parámetros. Usar exclusivamente los campos permitidos del contrato. No enviar términos ni contenido libre a las etiquetas. Configurar los identificadores reales mediante el mecanismo de runtime existente, sin valores ficticios en código.

`trial_started` y `subscription_started` necesitan estados confirmados del backend, deduplicación y atribución persistida; no se disparan desde pantallas ni desde la elección de un plan.

## Propuesta de fase 5 — requiere aprobación antes de implementar

- Nueva entidad `AdquisicionEmpresaEntidad`, separada de `EmpresaEntidad`, con una relación única por `empresa_id` hacia la tabla existente `empresa`. Campos opcionales: source, medium, campaign, content, term, landing_vertical, first_landing_path y acquired_at. La ruta inicial contendría solamente una ruta válida de landing, sin query ni fragmento; la fecha la asigna el servidor.
- Migración aditiva: nueva tabla, FK e índice único por empresa. Sin alteración ni relleno artificial de registros históricos; los negocios existentes no tendrán atribución hasta disponer de evidencia válida.
- DTO opcional `AtribucionAdquisicionRequest` dentro de `RegistrarEmpresaRequest`, con validación backend independiente del frontend. El alta actual seguirá aceptando solicitudes sin atribución.
- Reutilizar `POST /api/v1/onboarding/empresas` y `ServicioOnboardingEmpresaImpl`: guardar atribución dentro de la misma transacción que crea el negocio. Ningún endpoint público adicional ni posibilidad de reasignar la atribución de otra empresa.
- Separar el servicio de persistencia comercial del servicio de planes y facturación. Los eventos de trial y suscripción usarán estados confirmados de esos servicios; no concederán beneficios ni modificarán decisiones comerciales.
- Lectura futura en informes de plataforma, con permiso administrado por backend. No exponer la atribución en el sitio público del tenant.

Esta propuesta no se aplicó al esquema ni a los endpoints. La medición completa y persistencia histórica siguen pendientes.
