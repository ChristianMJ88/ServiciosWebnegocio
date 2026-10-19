# 008 F4 — recursos heredados locales

Autor: Codex. Revisión solo lectura, sin modificar aplicación ni revisiones anteriores.

Se implementa el P1 de `008-f4-rendimiento-pendiente.md`: Playfair Display v40 peso 700, mismos cuatro subconjuntos y unicode-range de la hoja actual, pasa a WOFF2 locales con licencia OFL oficial. Misma familia y swap, sin preload. Poppins no tiene consumidores según `rg` y desaparece de ambas entradas HTML. Animate.css global pasa a tres efectos usados por Booking (fadeIn/headShake/fadeInUp), copiados del CSS oficial 4.1.1, con versiones webkit y normales, timings y licencia MIT completos. Base conserva duración 1s/fill both; sin variables globales que nadie sobrescribía. El componente incluye el archivo y su reduced-motion existente sigue dominando con animation:none!important.

Archivos: `src/index.html`, `src/index.app.html`, `src/styles.css`, `shared/styles/tenant-fonts.css`, `pages/booking/booking-animate.css` y metadata de estilos del componente; binarios/licencia/README en public/fonts/tenant; README marketing actualizado. Sin cambios de contratos, selectores HTML, familias configurables, clases de negocio, host, backend, migración ni datos. No se añaden fuentes que antes no estuvieran disponibles.

Guarda `npm run check:public-assets` aprobada: cuatro WOFF2 con cabecera válida, reglas Unicode, URLs locales, ningún CDN heredado en ambas entradas, cada efecto usado presente y scoped. Suite Angular completa posterior: 252 pruebas / 78 archivos aprobados, exit 0, un worker temporal, sin ampliar timeouts. Builds y UI en curso; se registrarán resultados reales, sin prometer Performance 85. Se genera stats.json para diagnosticar el bundle, sin cambiar toolchain ni separar CSS global.

Revisa copia/licencias, compatibilidad/encapsulación de animaciones y aislamiento de las fuentes. No se afirma todavía una captura real con tenant PLAYFAIR: el tenant local disponible usa INTER. No se modifica la configuración de un negocio para forzar esa prueba.
