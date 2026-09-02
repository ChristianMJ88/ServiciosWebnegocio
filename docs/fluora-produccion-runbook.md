# Fluora: arquitectura, despliegue y operación de producción

Última actualización: 28 de agosto de 2026.

Este documento registra el estado real de producción después de la migración del backend, la configuración de dominios, el despliegue de Firebase y la implementación de SEO por empresa. No contiene contraseñas, tokens ni valores de secretos.

## 1. Producto y estrategia

La marca visible es **Fluora**. El dominio actual conserva `refluora.com`.

Fluora reúne agenda de citas, minisito por empresa, CRM, clientes, recepción, caja, servicios, precios, promociones, equipo y comunicaciones. WhatsApp es un canal integrado para organizar mensajes y citas, no el producto principal.

La landing usa agenda y minisito como entrada comercial de menor fricción. Su rediseño incluyó marca unificada, tema oscuro, CTA de prueba, agenda animada con GSAP y confirmación de cita, mockups responsive, navegación móvil, footer más humano y microcopia para negocios pequeños y medianos.

## 2. Cuentas y responsabilidades

| Recurso | Cuenta operativa |
| --- | --- |
| Firebase, Hosting y Functions | `mejiac504@gmail.com` |
| Google Cloud del backend | `christianmejia@tecprotech.com.mx` |
| Cloudflare de `refluora.com` | `christianmejia@tecprotech.com.mx` |
| Proyecto Firebase | `fir-serviciosweb-b2901` |
| Proyecto del backend | `refluora-prod` |

No mezclar despliegues: frontend y SEO se administran con la cuenta Gmail; API, MySQL y DNS con la cuenta corporativa.

## 3. Arquitectura desplegada

```text
Usuarios y buscadores
  |
  +-- refluora.com ---------------- Firebase Hosting: marketing
  |     +-- / ---------------------- Angular SPA
  |     +-- /e/{slug} -------------- Function tenantPage
  |     +-- /sitemap.xml ------------ Function sitemap
  |
  +-- app.refluora.com ------------- Firebase Hosting: app
  |
  +-- api.refluora.com ------------- Cloud Run domain mapping
        +-- Spring Boot ------------ Cloud SQL MySQL
```

## 4. Dominios y DNS

| Host | Destino | Proxy | Uso |
| --- | --- | --- | --- |
| `refluora.com` | `199.36.158.100` | Solo DNS | Hosting marketing |
| `www.refluora.com` | `fir-serviciosweb-b2901.web.app` | Solo DNS | Hosting marketing |
| `app.refluora.com` | `fir-serviciosweb-b2901.web.app` | Solo DNS | Hosting app |
| `api.refluora.com` | `ghs.googlehosted.com` | Solo DNS | Cloud Run |
| `sistemacrud.refluora.com` | túnel `backend-serviciosweb` | Con proxy | Sistema independiente |

`api.refluora.com` antes apuntaba al túnel compartido y consultaba el backend equivocado. Se creó un domain mapping de Cloud Run y se reemplazó únicamente ese registro. `sistemacrud.refluora.com` no fue modificado.

`refluora.com` quedó verificado en Google Search Console mediante TXT. El registro debe conservarse. El certificado de `api.refluora.com` es administrado por Google.

```bash
dig @1.1.1.1 +short CNAME api.refluora.com
```

Resultado esperado: `ghs.googlehosted.com.`

## 5. Backend en Google Cloud

| Campo | Valor |
| --- | --- |
| Proyecto | `refluora-prod` |
| Servicio Cloud Run | `fluora-agenda-api` |
| Región | `us-central1` |
| Dominio | `https://api.refluora.com` |
| API base | `https://api.refluora.com/api/v1` |
| Cuenta de ejecución | `fluora-backend-runtime@refluora-prod.iam.gserviceaccount.com` |

La URL directa de Cloud Run queda como contingencia, pero frontend y Functions usan el dominio estable.

```bash
curl https://api.refluora.com/actuator/health
curl https://api.refluora.com/api/v1/publico/sitio/indice
```

Estado validado:

```json
{"status":"UP","groups":["liveness","readiness"]}
```

```json
{"slugs":["nail-art"]}
```

Si aparecen empresas del sistema anterior, revisar primero el CNAME de `api.refluora.com`.

## 6. Cloud SQL y secretos

| Campo | Valor |
| --- | --- |
| Instancia | `fluora-agenda-mysql` |
| Motor | MySQL 8 |
| Zona | `us-central1-f` |
| Base | `agenda_db` |
| Usuario | `agenda_user` |
| Clase | `db-f1-micro` |
| Almacenamiento | 10 GB |
| Backups | Desactivados por decisión actual |
| Auto-resize | Desactivado por decisión actual |

Secret Manager contiene `fluora-db-password` y `fluora-jwt-secret`. Nunca documentar sus valores. Producción valida el esquema con Hibernate y lo evoluciona mediante Flyway. Se agregó `V44__regla_disponibilidad_dia_semana_int.sql`.

La configuración prioriza costo bajo. Antes de almacenar información crítica se deben activar backups y definir restauración.

## 7. Backend local

El MySQL correcto es el servicio del proyecto de agenda. Se expone en `localhost:3302` para no chocar con `3306`.

```bash
docker compose up -d mysql
docker compose ps
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}'
```

No conectar el backend al contenedor de otro proyecto. Las migraciones Flyway no deben editarse después de aplicarse. Durante la migración se verificaron 18/18 pruebas del backend.

## 8. Firebase Hosting

| Target | Site ID | Dominio |
| --- | --- | --- |
| `marketing` | `fir-serviciosweb-b2901` | `refluora.com`, `www.refluora.com` |
| `app` | `fluora` | `app.refluora.com` |

Cada target publica un artefacto independiente desde el mismo workspace Angular:

- `marketing`: `dist/marketing/browser` para landing, legales, minisitios y reservas públicas.
- `app`: `dist/application/browser` para registro, acceso y paneles privados.

```bash
npm run build
firebase deploy --only hosting:marketing,hosting:app --project fir-serviciosweb-b2901
```

`public/runtime-config.js` publica:

```js
window.__AGENDA_API_BASE_URL__ = 'https://api.refluora.com/api/v1';
window.__FLUORA_MARKETING_ORIGIN__ = 'https://refluora.com';
window.__FLUORA_APP_ORIGIN__ = 'https://app.refluora.com';
```

Validar después de desplegar:

```bash
curl https://refluora.com/runtime-config.js
curl https://app.refluora.com/runtime-config.js
```

## 9. SEO por empresa

Cada empresa activa puede tener `https://refluora.com/e/{slug}`. El ejemplo productivo es [nail-art](https://refluora.com/e/nail-art).

Firebase Hosting reescribe la ruta hacia `tenantPage`. La Function obtiene datos públicos del backend e inyecta en el HTML inicial:

- título y descripción;
- canonical;
- Open Graph para Facebook y WhatsApp;
- Twitter Card;
- imagen social del negocio;
- aplicación Angular intacta.

Así, al compartir el minisito se genera una tarjeta con el nombre, descripción e imagen de la empresa, no metadatos genéricos.

`https://refluora.com/sitemap.xml` es dinámico. Consulta el índice público e incluye páginas base y slugs válidos sin duplicados. Se validaron `/`, `/registro` y `/e/nail-art`.

| Function | Runtime | Región | Acceso |
| --- | --- | --- | --- |
| `tenantPage` | Node.js 22, 2nd Gen | `us-central1` | Público |
| `sitemap` | Node.js 22, 2nd Gen | `us-central1` | Público |

El permiso `roles/run.invoker` para `allUsers` es intencional solo en estas dos Functions públicas.

```bash
npm --prefix functions test
npm --prefix functions run prepare-template
firebase deploy --only functions --project fir-serviciosweb-b2901
```

El script debe llamarse `prepare-template`, no `prepare`: npm ejecuta `prepare` dentro de Cloud Build, donde no existe el build Angular completo.

```bash
curl -I https://refluora.com/e/nail-art
curl -I https://refluora.com/sitemap.xml
```

Ambos deben responder 200; el tenant usa `text/html` y el sitemap `application/xml`.

## 10. Facturación y costos

Firebase usa Blaze para Functions de segunda generación. `fir-serviciosweb-b2901` quedó vinculado a la cuenta de facturación corporativa.

La cuenta tenía cuota de tres proyectos. Para liberar espacio se desvinculó, sin eliminarlo, `project-1166c9d0-5fb8-4145-82f` (`My First Project`). Antes se comprobó que no tenía Cloud Run, Cloud SQL, buckets, BigQuery ni Firestore activos.

Quedaron vinculados `gestionar-de-tienda`, `refluora-prod` y `fir-serviciosweb-b2901`.

Artifact Registry elimina imágenes de Functions con más de 7 días:

```bash
firebase functions:artifacts:setpolicy --location us-central1 --days 7 --force \
  --project fir-serviciosweb-b2901
```

## 11. Incidencias resueltas

- **API con datos equivocados:** el DNS apuntaba al túnel anterior; se cambió a Cloud Run.
- **404 en slug y sitemap:** los rewrites existían, pero faltaban Functions y Blaze.
- **Fallo de Cloud Build:** el script reservado `prepare` buscaba `/dist`; se renombró.
- **403 en Functions:** faltaba invocación pública en los dos servicios SEO.
- **Costo acumulativo de imágenes:** se agregó retención de 7 días.
- **Node 20 próximo a retiro:** Functions se actualizaron a Node 22.

## 12. Commits principales

```text
4ae33e9  feat: rediseñar landing de Fluora
54d5c3a  fix: mejorar experiencia movil de la landing
98dd4d4  feat: enfocar landing en agenda y añadir SEO por negocio
82a9a01  feat: animar reservas en el hero con GSAP
caa079b  feat: confirmar citas en la animacion del hero
2e7be65  feat: refinar animacion editorial del hero
a59f0df  fix: incluir conector de Cloud SQL en backend
22cec7d  fix: validar esquema de disponibilidad en produccion
f2701b8  chore: conectar firebase al backend de fluora
34cdca6  chore: usar dominio de produccion para la api
120c140  fix: desplegar funciones seo con node 22
```

## 13. Checklist de despliegue

1. Revisar `git status` y preservar cambios ajenos.
2. Ejecutar pruebas del backend si cambió Java.
3. Ejecutar `npm run build`.
4. Ejecutar pruebas de Functions si cambió SEO.
5. Desplegar backend y validar health antes del frontend.
6. Desplegar Hosting y, cuando aplique, Functions.
7. Validar ambos `runtime-config.js`.
8. Validar API, `/e/nail-art` y `/sitemap.xml`.
9. Revisar título, canonical, descripción y Open Graph.
10. Crear un commit limitado; no incluir cachés ni archivos ajenos.

## 14. Pendientes

- Activar backups de Cloud SQL antes de manejar datos críticos.
- Crear alertas de presupuesto para backend y Firebase.
- Enviar el sitemap en Search Console y revisar cobertura.
- Sustituir la imagen demo por imagen propia de cada empresa cuando exista.
- Formalizar unicidad, normalización, redirecciones y palabras reservadas de slugs.
- Implementar flujo operativo antes de aceptar dominios propios de clientes.
- Auditar y limpiar, con autorización explícita, el Firebase/site accidental `fluora-agenda-app` creado en `refluora-prod`; no se usa en producción.
- Mantener `sistemacrud.refluora.com` fuera del alcance de Fluora.

## 15. Seguridad

- No versionar `.env`, contraseñas, tokens, JWT ni credenciales SMTP.
- No imprimir valores de Secret Manager.
- Mantener separadas las cuentas de Firebase y backend.
- Limitar `allUsers` a los dos endpoints SEO públicos.
- Mantener `api.refluora.com` como Solo DNS para el dominio administrado de Google.
- Confirmar recurso y dependencias antes de eliminar o desvincular infraestructura.

## 16. Identidad social de Microsoft

El inicio de sesión de usuarios con Microsoft es independiente del conector Microsoft Graph que cada tenant puede usar para enviar correo. No deben intercambiar variables ni callbacks.

Variables exclusivas del acceso social:

```text
AUTH_SOCIAL_MICROSOFT_HABILITADO=true
AUTH_SOCIAL_MICROSOFT_CLIENT_ID=<id de la aplicación de identidad>
AUTH_SOCIAL_MICROSOFT_CERTIFICATE_THUMBPRINT=<huella del certificado>
AUTH_SOCIAL_MICROSOFT_PRIVATE_KEY_PEM=<llave privada desde Secret Manager>
AUTH_SOCIAL_MICROSOFT_REDIRECT_URI=https://api.refluora.com/api/v1/auth/social/microsoft/callback
AUTH_SOCIAL_FRONTEND_REGISTRO_URL=https://app.refluora.com/registro
AUTH_SOCIAL_FRONTEND_ACCESO_URL=https://app.refluora.com/acceso
```

En Microsoft Entra, la aplicación de identidad debe admitir organizaciones múltiples, tener permiso delegado `User.Read` y registrar exactamente el URI de callback anterior. La llave privada nunca se versiona: se suministra desde Secret Manager.

El callback entrega un código aleatorio con vigencia de cinco minutos. El código se guarda mediante hash, se consume una sola vez y permite seleccionar empresa cuando la misma identidad pertenece a varios tenants.
