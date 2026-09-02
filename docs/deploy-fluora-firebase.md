# Despliegue de Fluora en Firebase

La arquitectura propuesta originalmente ya fue implementada. La documentación canónica y actualizada está en:

- [Fluora: arquitectura, despliegue y operación de producción](./fluora-produccion-runbook.md)

## Estado real

- Proyecto Firebase: `fir-serviciosweb-b2901`
- Target marketing: site `fir-serviciosweb-b2901`
- Target app: site `fluora`
- Marketing: `https://refluora.com`
- App: `https://app.refluora.com`
- API: `https://api.refluora.com/api/v1`
- Minisitos: `https://refluora.com/e/{slug}`
- Sitemap: `https://refluora.com/sitemap.xml`

El identificador `fluora` y su URL técnica `fluora.web.app` pertenecen únicamente a Firebase Hosting. No representan un dominio adicional de producto: el acceso público de la aplicación es `app.refluora.com`.

## Hosting

```bash
npm run build
firebase deploy --only hosting --project fir-serviciosweb-b2901
```

## Functions SEO

```bash
npm --prefix functions test
npm --prefix functions run prepare-template
firebase deploy --only functions --project fir-serviciosweb-b2901
```

## Validación

```bash
curl https://refluora.com/runtime-config.js
curl -I https://refluora.com/e/nail-art
curl https://refluora.com/sitemap.xml
curl https://api.refluora.com/actuator/health
```

No usar los nombres históricos `refluora-app`, `fluora-marketing` o `fluora-app` como IDs reales de Hosting. Los targets vigentes se encuentran en `.firebaserc`.
