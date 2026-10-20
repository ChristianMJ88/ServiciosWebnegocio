# Revisión acotada de implementación CLS

Claude: solo lectura, sin ejecutar comandos ni modificar aplicación. Revisa únicamente `src/app/app.ts`, `app.html`, `app.css`, `app.spec.ts`, `src/app/components/header/header.component.ts` y su nuevo spec, contrastando `docs/agentes/revisiones/008-f4-cls-diseno-2.md`. Usuario priorizó CLS tenant; sin M3/hosting/commit/datos/backend/RBAC. El árbol contiene cambios previos M1/M2 y ajenos: no revisarlos globalmente.

Implementado D1–D3. Consultas string + ElementRef; outlet vivo, inert/aria-busy durante espera; fallback accesible de navegación al fallar chunk. Se conserva HeaderComponent en imports de @Component: Angular necesita declararlo para el template, quitarlo produciría NG8001. No hay referencias de clase fuera de imports/template en aplicación. El build de producción debe acreditar chunk separado (pendiente aún), pruebas manuales de defer pasan.

Desviación explícita del pseudocódigo: @error contiene enlaces y recarga en lugar de div vacío oculto, sin altura inventada. Se añade estado accesible de carga fijo. Ninguna regla de negocio nueva.

19 pruebas focales verdes antes de ampliar conservación precargada a host propio; suite completa en curso. Revisa corrección funcional, herencia de visibility, activación inicial, fallos de navegación, paso SPA entre tenants/salida marketing. No apruebes métricas por adelantado: después se medirán Home/Servicios/Agendar/Checkout390/820/1440 frío/caliente, con trazas y APIs reales de solo lectura. Home85 sigue abierto.

Devuelve hallazgos concretos con prioridad o sin bloqueantes y límites. Respuesta breve; no necesitas leer más archivos que los indicados y dependencias puntuales si una duda lo requiere.
