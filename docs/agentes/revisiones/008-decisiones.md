# 008: decisiones del usuario y orden de trabajo

Autor: Claude Code, 2026-10-18. Estas decisiones completan `revisiones/008.md` §9. El diseño, las fases y los criterios de aceptación de 008 siguen igual. Prototipo de referencia: `revisiones/propuesta-home-fluora.md`.

## Decisiones
- **D1, titular y público:** opción A. «La agenda en línea para salones, barberías y spas.»
- **D2, prueba social:** se omite. La franja no se muestra y no se inventan cifras, logos ni testimonios.
- **D3, promesas comerciales que el usuario confirmó como ciertas:**
  - se puede probar sin tarjeta;
  - sin contrato, y se puede cancelar en cualquier momento;
  - ayudan a migrar desde otra agenda;
  - **nueva, aportada por el usuario:** acompañan todo el proceso de alta del negocio para sacarle el máximo rendimiento. Redactarla breve y sin cifras, por ejemplo: «Te acompañamos en la configuración de tu negocio para que le saques el máximo provecho desde el primer día». Puede ir como respuesta del FAQ y como línea de apoyo cerca del CTA final;
  - **horario de soporte:** el usuario lo dio el 2026-10-18 y pidió que **no quede en el código**, porque lo cambiará él mismo. Se configura según la sección «Soporte configurable» de abajo.
- **D5, tienda en línea (2026-10-18):** el usuario pidió que el home diga que, desde el sitio que crean al registrarse, los negocios también pueden montar **su propia tienda** para vender sus productos, con inventario, finanzas, carrito de compras, pagos en línea y métodos de envío. Lo verifiqué en el código: existen los módulos `PRODUCTOS`, `INVENTARIO`, `PAGOS_ONLINE` y `ENVIOS` (`CodigosModuloPlataforma.java`), el checkout de Mercado Pago, las cotizaciones y guías de envío (V98/V99) y los reportes financieros de 005. **Son módulos que dependen del plan** (`RequiereModulo`), así que el texto no puede prometerlos en todos los planes. Cambios sobre `008.md` §2–§3:
  - **Terminología:** en el texto se dice **«tienda en línea»**. En el código, «marketplace» es el **directorio de negocios de Fluora** (`/api/v1/publico/marketplace`, bloque 7), que es otra cosa. No se mezclan los dos términos.
  - **Kicker del hero:** «Agenda · Sitio · Tienda · Caja · CRM».
  - **Lead:** «Tus clientes reservan y compran desde tu propio sitio; tu equipo trabaja sin cruces y cobras desde la misma app.» Se mantiene el criterio de **un máximo de 2 líneas a 375 px**. Si no cabe, se queda el lead actual y la tienda aparece en los pilares.
  - **Pilares:** pasan de 3 a **4**. El nuevo es **«Vende en línea»: tienda en tu sitio con carrito, pagos en línea y envíos; inventario y finanzas en el mismo panel.** En escritorio, 4 tarjetas en una fila (o 2×2 en tablet); en móvil, lista compacta.
  - **Cómo funciona, paso 1:** «Regístrate y publica tu sitio con agenda y tienda».
  - **Producto en pestañas:** una pestaña más, **«Tienda en línea»**, con catálogo → carrito → pago → envío, e inventario y finanzas en el panel.
    - Solo se usa una **captura real** de un tenant demo con datos de demostración. Si no existe, la pestaña va sin imagen, con un esquema de pasos.
    - Con el patrón de pestañas de WAI-ARIA, igual que las demás.
  - **FAQ:** una pregunta nueva, «¿Puedo vender mis productos?». Se responde con lo de arriba y se indica que **depende del plan**, con enlace a `#planes`. Se mantiene el máximo de 6 o 7 preguntas: si hace falta, se juntan dos.
  - **Planes:** qué plan incluye tienda, inventario, pagos en línea y envíos sale de la **API de planes** (sus módulos), no del texto. Si la API todavía no expone los módulos por plan, Codex lo reporta antes de escribir algo fijo.
  - **Envíos y pagos:** no se nombran paqueterías ni proveedores, salvo los que el sistema realmente integra (Mercado Pago sí; las paqueterías, Codex las verifica en V98/V99 y en el código).
  - **Criterio:** el home móvil sigue midiendo ≤ 7 pantallas de 812 px con el pilar y la pestaña nuevos.
- **D4, «Quiénes somos»:** no hay bloque. Basta con el pie de página, porque no hay un texto aportado por el usuario.

## Soporte configurable (F0 de 008, backend y plataforma)

Valores iniciales que dio el usuario:
- de lunes a viernes, de 09:00 a 17:00;
- correo `soporte@refluora.com`;
- teléfono `2213688033`.

Zona horaria: **America/Mexico_City** (hora del centro de México), confirmada por el usuario el 2026-10-18.

**Ningún dato de soporte se escribe en código, plantillas ni textos.** Además, hoy el pie de página en modo Fluora trae fijo `contacto@refluora.com` (`footer.component.html:100`). Ese también se cambia para que salga de esta configuración.

### Diseño
1. **Dónde vive.** En el módulo `marketing`, que ya administra las landings de la plataforma con el permiso efectivo existente `PLATAFORMA_MARKETING_GESTIONAR` (`ControladorLandingPlataforma.java:11`). No se crean permisos nuevos.
2. **Datos.** Una sola configuración global de la plataforma, sin tenant:
   - `correo`: validado y en minúsculas;
   - `telefono`: guardado normalizado, por ejemplo en E.164 `+522213688033`, y mostrado con formato local;
   - `dias`: un conjunto de días de la semana;
   - `horaInicio` y `horaFin`: `LocalTime`, con `fin > inicio`;
   - `zonaHoraria`: IANA, validada con `ZoneId`;
   - `visible`;
   - `actualizadoEn` (UTC) y `actualizadoPor`.

   Sigue el patrón de tablas de configuración que ya existe. La migración es **V140**: Codex la **reserva primero** en ESTADO.md con `git fetch`, como con V137–V139. La migración puede **sembrar** una fila con los valores iniciales del usuario: es un dato editable, no está en el código. Si se prefiere no sembrar, el usuario la captura desde la plataforma.
3. **API de plataforma.**
   - `GET /api/v1/plataforma/marketing/soporte` y `PUT /api/v1/plataforma/marketing/soporte`, con `PLATAFORMA_MARKETING_GESTIONAR`.
   - El PUT valida todo en el backend, registra el cambio en `ServicioAuditoriaPlataforma` (valores anterior y nuevo) y usa `version` o `actualizadoEn` como control optimista: si la versión es vieja, responde 409.
4. **API pública.**
   - `GET /api/v1/publico/plataforma/soporte` devuelve solo correo, teléfono, días, horario y zona, con caché de 60 s.
   - Si `visible=false` o no hay configuración, responde 404 o un cuerpo vacío. **No hay un respaldo escrito en el código.**
5. **Editor.** En `platform-marketing`, una sección «Soporte» con:
   - correo;
   - teléfono;
   - casillas de días;
   - hora de inicio y de fin;
   - zona;
   - visible;
   - vista previa del texto público.

   Los errores se muestran con el `mensaje` del backend.
6. **Dónde se muestra:** solo en las páginas de Fluora, **nunca** en los minisitios de tenant (`/e/:slug` y hosts propios siguen con el contacto del negocio).
   - **Pie de página Fluora:** correo (`mailto:`), teléfono (`tel:`) y el texto del horario. Si no hay configuración, se oculta el bloque.
   - **FAQ del home:** la respuesta sobre soporte, por ejemplo «Atendemos de lunes a viernes de 9:00 a 17:00 (hora del centro de México) en soporte@refluora.com y al 221 368 8033», se compone con los datos de la API. Si no hay configuración, la pregunta no se muestra.
   - El texto de días y horario lo arma una función compartida a partir de los campos estructurados. No va escrito en el HTML. El horario se presenta en la zona configurada.
7. **SSR y SEO:** si el home se prerenderiza, el dato se obtiene en el servidor o se hidrata sin que la página salte de lugar. Comprobar CLS < 0.1 (criterio de 008).

### Pruebas
- Backend:
  - PUT con y sin permiso (200/403);
  - validaciones: correo inválido, `fin ≤ inicio`, zona inválida, ningún día;
  - 409 por versión;
  - auditoría registrada;
  - la API pública no expone `actualizadoPor` ni campos internos;
  - `visible=false` → oculto.
- Angular:
  - el editor envía y muestra errores;
  - el pie de página Fluora muestra los datos de la API y se oculta sin ellos;
  - el pie de página del tenant no cambia (regresión);
  - el FAQ compone el texto y oculta la pregunta sin datos;
  - un formato de días distinto de lunes a viernes genera el texto correcto.
- Búsqueda en el repo sin coincidencias de `soporte@refluora.com`, `2213688033` ni `contacto@refluora.com` en `src/` ni en `backend/src/main` (salvo la siembra de V140, si se usa).

## Orden respecto a lo demás
F1–F4 de 008 son solo frontend de marketing. **F0 (soporte configurable) sí toca backend, con la migración V140 y una configuración editable**; va antes de F1 y se cierra con su propia revisión. Todo 008 **va después** de:
1. el commit de 010, autorizado por el usuario el 2026-10-18 (ver `010-aprobacion.md`, adenda);
2. O-H1, el usuario MySQL de pruebas limitado a esquemas efímeros;
3. la línea base de 011, es decir, las carpetas 00–06 en verde.

Las fases F1–F4 se cierran **una por una**, cada una con su respuesta de Codex y una revisión de Claude, tal como dice `008.md` §7.

## Restricciones adicionales
- Tenant intacto: `/e/:slug` y los hosts propios no cambian. Se comprueba con capturas de antes y después.
- Ninguna promesa adicional a las de D3. Si el texto necesita otra, se le pregunta al usuario.
- Sin commit, push ni despliegue de 008 sin autorización.
