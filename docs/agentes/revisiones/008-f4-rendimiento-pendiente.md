Tengo suficiente evidencia y estoy armando la revisión. No tengo herramienta de escritura en esta sesión, así que no pude crear `docs/agentes/revisiones/008-f4-rendimiento-pendiente.md`; la revisión va abajo para que alguien la guarde con ese nombre.

---

# Revisión 008 F4: rendimiento pendiente

Solo leí archivos. No edité nada, no ejecuté builds ni Lighthouse y no hice commits. **Estoy de acuerdo en que el criterio de 85 sigue sin cumplirse.** Este cambio debería mejorar FCP/LCP de forma medible, pero **casi seguro no basta para llegar a 85**, porque TBT (420–800 ms) es un problema aparte. Lo separo abajo.

## Evidencia: qué usa de verdad cada hoja heredada

| Recurso (`src/index.html`) | Quién lo usa | Conclusión |
|---|---|---|
| `:26` Google Fonts: **Poppins** 300/400/600 | **Nadie.** No aparece en ningún `font-family` de `src/` ni en `backend/src/main`. `resolverFuenteCss` (`src/app/app.ts:153-166`) solo ofrece INTER, JAKARTA, PLAYFAIR, MANROPE y SYSTEM. | Se puede quitar sin ningún cambio visual. |
| `:26` Google Fonts: **Playfair Display** 700 | Tenants con `fuenteTitulos/Cuerpo = PLAYFAIR` (`app.ts:158`) y la vista previa del editor de sitio (`admin-site-section.component.css:131`, `.ts:187`; build application). | Alojarla localmente con el mismo nombre de familia. |
| `:28` animate.css 4.1.1 (cdnjs) | **Solo Booking:** `booking.component.html:268, 326, 343, 424, 490`. Son 3 animaciones: `fadeIn`, `headShake` y `fadeInUp`. No se usa en el backend ni en el contenido. | Llevar las 3 animaciones al componente Booking. |
| `:24` preconnect `fonts.gstatic.com` | Solo tiene sentido con Google Fonts. | Quitarlo cuando ya no se use Google Fonts. Si no, Lighthouse lo marca como preconnect sin usar. |

Ahora mismo **todos los visitantes**, incluida la Home de Fluora, que no usa ninguna de las dos hojas, pagan dos orígenes de terceros (DNS+TCP+TLS) con CSS que bloquea el render. Además, Google Fonts encadena la petición a gstatic. Con la limitación Slow 4G de Lighthouse, cada origen nuevo cuesta varios RTT antes del primer pintado.

## Recomendación P1: el cambio mínimo de frontend

Recomiendo **alojar Playfair localmente y llevar a Booking solo lo que usa de animate.css**. No conviene alojar animate.css completo: así seguiría siendo CSS global, aunque ya no fuera de terceros, y lo pagaría también la Home.

**1. Playfair Display alojada localmente (SIL OFL 1.1)**
- Pedir una vez el CSS de `fonts.googleapis.com/css2?family=Playfair+Display:wght@700&display=swap` con el user-agent de un Chrome actual. Copiar **todos** los bloques `@font-face` que devuelve (latin, latin-ext y los demás subconjuntos) con su `unicode-range`, `font-weight:700` y `font-display:swap`, sin `local()`, igual que Google.
- Los archivos van en `public/fonts/tenant/playfair-display-700-<subset>.woff2`, junto con `PlayfairDisplay-OFL.txt`.
- Las reglas `@font-face` van en `src/app/shared/styles/tenant-fonts.css`, importado desde `src/styles.css` al lado de `marketing-tokens.css:7`. `styles.css` es compartido, así que cubre los dos builds.
- **Mantener el nombre de familia exacto `"Playfair Display"`.** Aquí no aplica el alias de marketing, porque los tenants la referencian por ese nombre.
- Un `@font-face` no descarga nada hasta que un elemento usa esa familia. La Home y los tenants con INTER/JAKARTA/MANROPE no pagan ningún byte. **No agregar preload.**
- Quitar `index.html:24`, `:26` e `index.app.html:14`. Hay que quitarlos en los dos archivos: si `index.app.html` conserva Google, existirían dos `@font-face` para la misma familia y podría haber descargas duplicadas.
- Actualizar `public/fonts/marketing/README.md:5`, que hoy dice que Playfair se queda en `index.html`. Documentar la fecha de descarga y la versión de la fuente.

**2. animate.css: solo las 3 animaciones, en Booking (MIT)**
- Crear `src/app/pages/booking/booking-animate.css` y agregarlo a `styleUrls` (`booking.component.ts:67`). Debe conservar la cabecera de licencia MIT de animate.css 4.1.1.
- Copiar tal cual `.animate__animated` (`animation-duration:1s; animation-fill-mode:both`), `.animate__fadeIn`, `.animate__headShake` (incluido su `animation-timing-function:ease-in-out`), `.animate__fadeInUp` y los tres `@keyframes`.
- **No copiar el bloque `:root{--animate-*}`.** Con la encapsulación emulada, `:root` no coincidiría con nada dentro del componente. Hay que poner `1s` directamente; es equivalente, porque ningún archivo sobrescribe `--animate-duration`.
- Para reduced motion ya está `booking.component.css:1405` (`animation:none !important`), y eso gana sobre la regla de animate. Se mantiene igual.
- Quitar `index.html:27-28` e `index.app.html:15`.

**Fuera del alcance:** no se toca ningún host, ninguna precarga de Home, ningún contrato, precio o permiso, ni SSR o backend. No se oculta contenido ni se cambia la configuración de Lighthouse.

**Efecto esperado:** el bloque de render-blocking de terceros desaparece de Home, `/e/:slug` y dominio propio. Espero una mejora clara de FCP/SI y algo de LCP. TBT no cambia. **No prometo 85** con esto.

## Regresiones y validación

1. **Build:** marketing y application con exit 0, sin errores de budget, reportando el delta de CSS/JS inicial. Hacer grep en `dist/*/browser/index.html` y comprobar que no aparece `googleapis`, `gstatic` ni `cdnjs`. Conviene dejar esta comprobación como script o paso de verificación para que no se reintroduzca.
2. **Suite completa de Angular en verde**, sin ampliar timeouts.
3. **Visual, en Chrome incógnito, a 375×812, 820×1180 y 1440×900, con capturas de antes y después:**
   - Tenant con fuente **PLAYFAIR**: los títulos son idénticos y Network muestra solo `/fonts/tenant/playfair-*.woff2` (latin y, si hay caracteres extendidos, latin-ext).
   - Tenant con INTER: no aparece ninguna petición de Playfair.
   - Home de Fluora: no aparece ninguna petición de Playfair.
   - Editor de sitio con tema elegante (build application): la vista previa se ve con Playfair.
4. **Booking:** las horas hacen fadeIn, el aviso de horario vacío hace headShake y el formulario hace fadeInUp. En DevTools, comparar `animation-name`, `animation-duration: 1s` y `fill-mode: both` antes y después. Con `prefers-reduced-motion` emulado, no debe haber animación.
5. **Datos (solo lectura y opcional):** confirmar que ninguna configuración de sitio de tenant guarda CSS libre que use Poppins. El modelo actual es un enum, así que no espero hallazgos.
6. **Lighthouse:** mismo perfil y equipo en reposo, **5 corridas** por URL (Home de plataforma, `/e/<slug>` y `/e/<slug>/agendar`), reportando mediana y rango de Perf/FCP/LCP/TBT. La auditoría *render-blocking resources* no debe listar orígenes de terceros.

## P2: ampliación de arquitectura (aparte, no entra en este cambio)

Con TBT de 420–800 ms, el 85 depende del JS y del CSS global. Antes de proponer cambios hace falta un diagnóstico medible:

- **A. Desglose del bundle inicial de marketing (927.61 kB).** Ejecutar `ng build -c production,marketing --stats-json` más un analizador, o `source-map-explorer`. El builder es `@angular-devkit/build-angular:browser` (webpack, `angular.json:18`). Registrar qué módulos entran en `main` y cuáles faltan por diferir. Sin este dato no recomiendo ninguna división concreta.
- **B. CSS global compartido.** `src/styles.css:1-6` lleva al build marketing el tema prebuilt de Material, Bootstrap completo, bootstrap-icons y las hojas de administración (`admin-site-section`, `admin-shell`, `operation-workspace`). Separar `styles` por configuración en `angular.json` reduciría parseo y recálculo de estilos. Pero los tenants y Booking usan Bootstrap y quizá algunas reglas de esas hojas, así que hace falta un inventario de selectores y revisión visual por tenant. Por el riesgo de cambios visuales, es una decisión aparte.
- **C. Migrar a `@angular/build:application` (esbuild).** Probablemente mejore el chunking y el tamaño inicial. Es un cambio de toolchain y necesita su propia solicitud.

Recomiendo hacer **A antes que B o C**. Si después de P1 y A la mediana sigue por debajo de 85, la siguiente solicitud debería partir de los datos de A.

## Observación, sin acción

Inter, Plus Jakarta Sans y Manrope, que los tenants pueden elegir, no se cargan como webfont: hoy cada visitante ve la fuente del sistema si no las tiene instaladas. Corregirlo cambiaría lo que ven los tenants, así que queda fuera de este alcance. Lo anoto solo para que no se confunda con una regresión de P1.
