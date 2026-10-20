# Confirmación de pruebas y paso B condicionado

Claude solo lectura; no ejecutar comandos. Contrastar revisiones008-f4-cls-precarga-diseno/codigo con booking.component.html, booking-shell.spec.ts y booking-calendar.component.spec.ts. No ampliar M3/backend/negocio/commit.

P2 DOM añadido: fixture real con servicio precargado y Subjects de precio/staff. Conserva .booking-flow, inert/aria-busy y estado externo «Preparando tu selección», revela al completar ambos; variante quote.error muestra botón reintentar fuera de inert, click solicita y recibe cotización real del mock. Pasaron33/3 focales antes del paso B. P3console.error espía en ambos tests de calendario, sin llamadas.

**Paso B ya cumple condición medida:** con paso A, SHA cbceea85601ed7e7c4fab05624c7e646c4f5165d9f2cff3b991fe368f634e320,820frío CLS.015043792 y caliente.000114321, BP100 ambos, sin error getApi. Informe `tmp/lighthouse/cls-selection-a820/`. Tracefrío registra exactamente dos desplazamientos83.140625px de booking-service-browser: [33,1126,754,54]→[33,1043,754,137]→[33,1126,754,54], score.007521896 cada uno. Eso corresponde a eliminar/reinsertar la nota durante la segunda cotización.

Se añadió el @else if(cotizando()) con texto neutral propuesto; no conserva cotización ni montos viejos, ni cambia CSS/alturas/precios. Clase booking-quote-status solo identifica la región. Las dos variantes DOM se ampliaron: reintentar con Subject pendiente, texto exacto neutral sin $, cotizacion null, shell sigue activo y luego se sustituye por respuesta backend. Suite completa posterior en curso, aún no afirmar cifras; métricas pasoB pendientes.

Confirmar que cierra tus hallazgos y que paso B es el diseñado. No aprobar métricas/suites/builds por adelantado. Dictamen breve y necesario.
