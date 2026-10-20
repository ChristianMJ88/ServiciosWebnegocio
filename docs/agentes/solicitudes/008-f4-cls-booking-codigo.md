# Revisión acotada de la carga inicial de Agendar

Claude, solo lectura. Contrasta `revisiones/008-f4-cls-booking-diseno.md` con cambios exclusivamente en `src/app/pages/booking/booking.component.ts`, .html, .css y .spec.ts. Sin M3/backend/negocio/commit.

Bandera catalogoInicialListo unidireccional; se revela en next/error de la petición vigente, después de hidratación/grupo. No se revela en finalize ni respuestas obsoletas. Varias sucursales sin selección/lista vacía/error de sucursal revelan. El shell conserva formulario y tiene visibility/inert/aria-busy; estado role=status visually-hidden fuera. No se oculta de nuevo al cambiar sucursal o tras éxito. Ocho pruebas nuevas Subject; suite completa en curso, sin cambiar los argumentos esperados de cotización/reserva.

No hay medición simultánea: serie anterior terminó Servicios/Inicio/Agendar, se interrumpió antes de medir Checkout por la necesidad de corregir Agendar. Puertos temporales libres y procesos propios Chrome/Lighthouse ausentes. No atribuir esa serie a la nueva bandera.

Lee también el cambio puntual de `scripts/lighthouse-tenant-cache.mjs`: --allow-quote opt-in se limita al POST cotizar de lectura ya autorizado por public-preview; no amplía otros writes. Se registra en resumen. El preview tiene pruebas que restringen el endpoint exacto. Se medirá URL con servicio precargado como dato informativo, sin reservar/cobrar; si queda CLS por oleadas de precios/profesionales/días se documenta como tu diseño indicó, sin tocar lógica ahora.

Devuelve defectos concretos o sin bloqueantes y límites. No ejecutar comandos ni aprobar métricas aún inexistentes. No hace falta revisar archivos ajenos.
