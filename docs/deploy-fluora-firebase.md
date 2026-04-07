# Deploy Refluora en Firebase

## Objetivo

Separar tres experiencias:

- `refluora.com`: marketing y venta del software
- `app.refluora.com`: acceso centralizado y paneles internos
- dominios o subdominios de tenant: sitio publico de cada empresa

## Fase 1 recomendada

Usar el mismo build Angular en dos dominios Firebase Hosting:

- `refluora.com`
- `app.refluora.com`

Y dejar los sitios publicos de tenant por ruta:

- `refluora.com/e/nail-art`
- `refluora.com/e/{slug}`

Ventajas:

- menos complejidad operativa
- no rompe el flujo actual
- permite validar login central y tenant publico por separado

## Fase 2 recomendada

Agregar dominios propios por empresa:

- `nailart.com`
- `reservas.cliente.com`

En esta fase el frontend resuelve tenant por `hostname` y el backend responde con la configuracion publica correspondiente.

## Configuracion sugerida en Firebase

1. Crear dos sitios Hosting dentro del mismo proyecto Firebase:
   - `marketing`
   - `app`

2. Mapear dominios:
   - `refluora.com` -> sitio `marketing`
   - `www.refluora.com` -> sitio `marketing`
   - `app.refluora.com` -> sitio `app`

3. Desplegar el mismo contenido compilado en ambos sitios.
4. Mantener los dominios propios de clientes conectados al sitio `marketing`, no al sitio `app`.

## Pasos concretos con tu proyecto actual

Asumiendo que tu proyecto de Firebase sigue siendo `fir-serviciosweb-b2901` y que el sitio actual es el principal:

1. Deja el sitio actual como `marketing`.
2. Crea un segundo sitio de Hosting para la app:

```bash
firebase hosting:sites:create refluora-app
```

3. Aplica targets:

```bash
firebase target:apply hosting marketing fir-serviciosweb-b2901
firebase target:apply hosting app refluora-app
```

4. Cambia tu `firebase.json` a modo multisite con dos targets:
   - `marketing` para `refluora.com` y `www.refluora.com`
   - `app` para `app.refluora.com`

5. En Firebase Hosting conecta dominios:
   - `refluora.com` -> `marketing`
   - `www.refluora.com` -> `marketing`
   - `app.refluora.com` -> `app`

6. Despliega ambos:

```bash
firebase deploy --only hosting:marketing
firebase deploy --only hosting:app
```

## Ejemplo de targets en `.firebaserc`

```json
{
  "projects": {
    "default": "fir-serviciosweb-b2901"
  },
  "targets": {
    "fir-serviciosweb-b2901": {
      "hosting": {
        "marketing": [
          "fluora-marketing"
        ],
        "app": [
          "fluora-app"
        ]
      }
    }
  }
}
```

## Ejemplo de `firebase.json`

```json
{
  "hosting": [
    {
      "target": "marketing",
      "public": "dist/ServiciosWebnegocio/browser",
      "ignore": [
        "firebase.json",
        "**/.*",
        "**/node_modules/**"
      ],
      "rewrites": [
        {
          "source": "**",
          "destination": "/index.html"
        }
      ]
    },
    {
      "target": "app",
      "public": "dist/ServiciosWebnegocio/browser",
      "ignore": [
        "firebase.json",
        "**/.*",
        "**/node_modules/**"
      ],
      "rewrites": [
        {
          "source": "**",
          "destination": "/index.html"
        }
      ]
    }
  ]
}
```

## Runtime config sugerido

En `public/runtime-config.js` puedes publicar:

- `__AGENDA_API_BASE_URL__`
- `__FLUORA_MARKETING_ORIGIN__`
- `__FLUORA_APP_ORIGIN__`

Eso permite que el frontend sepa si esta en marketing, app o un dominio propio de tenant.

Valores recomendados para este proyecto:

```js
window.__AGENDA_API_BASE_URL__ = 'https://api.refluora.com/api/v1';
window.__FLUORA_MARKETING_ORIGIN__ = 'https://refluora.com';
window.__FLUORA_APP_ORIGIN__ = 'https://app.refluora.com';
```

## Dominio propio por cliente

Modelo sugerido:

- tabla `empresa_sitio_config.dominio_principal`
- backend: `GET /api/v1/publico/sitio/host/{hostname}`
- frontend: resolver tenant por `window.location.hostname`

Regla de negocio recomendada:

- `refluora.com` y `www.refluora.com` siempre muestran el landing principal
- `app.refluora.com` siempre muestra acceso y paneles
- cualquier otro hostname publico conocido de cliente se resuelve como tenant

## DNS para cliente con dominio propio

Opciones:

- usar subdominio del cliente:
  - `reservas.cliente.com`
- usar dominio principal del cliente:
  - `cliente.com`

Ambos deben conectarse al sitio `marketing` de Firebase Hosting, porque ahi vive la SPA publica que luego resuelve el tenant por `hostname`.

Proceso tipico:

1. cliente agrega el dominio en Fluora
2. Fluora lo guarda como pendiente
3. se muestran registros DNS necesarios
4. cliente configura DNS
5. Firebase emite certificado SSL
6. Fluora marca el dominio como verificado y activo

## Recomendacion operativa

- empezar con `fluora.com` + `app.fluora.com`
- en tu caso inmediato: `refluora.com` + `app.refluora.com`
- mantener sitios publicos por `slug`
- activar dominios propios solo para clientes que ya esten listos
