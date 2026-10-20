# Diseño acotado: CLS restante dentro de Agendar

Claude solo lectura, no ejecutar comandos ni editar aplicación. Usuario priorizó corregir CLS del tenant. D1–D3 App/Header implementados y revisados; no M3/hosting/backend/RBAC/datos/commit. Antes de invocar esta solicitud debe haber terminado la medición en curso para evitar concurrencia.

Artefacto887c…: Servicios frío/caliente390=0/.000470,820=.000114/.000114,1440=0/.000098. Inicio también<.001. Agendar conserva CLS interno390≈.058,820≈.219,1440≈.0045. No es ya main0→72: Lighthouse identifica `div.col-lg-11 > div.card > div.row > div.col-lg-4` (`booking-cart-column`), que se desplaza al cargar el catálogo. Traces crudos y resumen en `tmp/lighthouse/cls-final-agendar/` (no hace falta leer binarios/grandes traces). Codex inspeccionará rectángulos antes de aplicar.

Lee `src/app/pages/booking/booking.component.ts` (loadingCatalog, ngOnInit, loadBranches/loadServices, hidratación), `.html`, `.css`, booking-experience.css, `.spec.ts`. No ampliar reglas de negocio. loadBranches activa loadingCatalog; next false, selecciona sucursal backend si procede y encadena loadServices; finalize de servicios acaba carga. El template pinta resumen/formulario/carrito antes del catálogo, luego crece.

Propuesta: una bandera de presentación de carga inicial únicamente, no una regla del catálogo. Mantener hero visible y todo el formulario/outlet vivo. Ocultar (`visibility`, inert/aria-busy) únicamente booking-shell hasta la primera carga real completa, sin alturas inventadas ni ocultarlo de nuevo durante cambios del usuario. Revelar también en errores/catálogo vacío/múltiples sucursales sin selección; preservar recuperación/edición posterior. Estado accesible de carga sin alterar geometría. Reutilizar estados existentes si es posible sin introducir carreras con la cadena sucursales→servicios. No añadir esperas por tiempo ni recalcular precios/anticipo/horarios.

Evalúa esta dirección o una alternativa más simple sustentada. Incluye condiciones exactas para primera carga, errores y peticiones obsoletas, y pruebas necesarias. Considera URL con sucursal/servicio precargado y cotización backend asíncrona (solo POST cotizar de lectura). No aceptar CLS por ocultar permanentemente contenido ni usar un min-height adivinado. Mantener diseño final y reservas/pagos idénticos.

Dictamen breve, hallazgos priorizados y alcance para Codex; métricas posteriores no aprobadas por adelantado.
