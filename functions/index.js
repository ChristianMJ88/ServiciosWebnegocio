'use strict';

const { onRequest } = require('firebase-functions/v2/https');
const { logger } = require('firebase-functions');
const { defineString } = require('firebase-functions/params');
const { renderNotFound, renderTenantHtml } = require('./seo-renderer');
const { createSitemapHandler } = require('./sitemap-handler');
const { renderLandingHtml } = require('./landing-renderer');
const { renderDirectoryHtml } = require('./directory-renderer');
const { loadCsrTemplate } = require('./csr-shell');
const appOriginParam = defineString('FLUORA_APP_ORIGIN');
const clientesOriginParam = defineString('FLUORA_CLIENTES_ORIGIN', { default: '' });
const { copyUpstreamHeaders } = require('./proxy-headers');

const apiBaseUrlParam = defineString('FLUORA_API_BASE_URL');
const marketingOriginParam = defineString('FLUORA_MARKETING_ORIGIN');
const backendOriginParam = defineString('FLUORA_BACKEND_ORIGIN');

function runtimeConfig() {
  const apiBaseUrl = apiBaseUrlParam.value().trim().replace(/\/+$/, '');
  const marketingOrigin = marketingOriginParam.value().trim().replace(/\/+$/, '');
  if (!apiBaseUrl || !marketingOrigin) {
    throw new Error('Faltan FLUORA_API_BASE_URL o FLUORA_MARKETING_ORIGIN');
  }
  return { apiBaseUrl, marketingOrigin };
}

exports.apiProxy = onRequest(
  { region: 'us-central1', memory: '256MiB', timeoutSeconds: 60, maxInstances: 20 },
  async (request, response) => {
    const backendOrigin = backendOriginParam.value().trim().replace(/\/+$/, '');
    if (!backendOrigin) {
      response.status(503).json({ mensaje: 'La API no está configurada' });
      return;
    }

    const targetUrl = `${backendOrigin}${request.originalUrl || request.url}`;
    const headers = {};
    for (const [name, value] of Object.entries(request.headers)) {
      if (!value || ['host', 'content-length', 'connection'].includes(name.toLowerCase())) continue;
      headers[name] = Array.isArray(value) ? value.join(', ') : value;
    }

    try {
      const method = request.method.toUpperCase();
      const upstream = await fetch(targetUrl, {
        method,
        headers,
        body: ['GET', 'HEAD'].includes(method) ? undefined : request.rawBody,
        redirect: 'manual',
        signal: AbortSignal.timeout(55000)
      });

      copyUpstreamHeaders(upstream.headers, response);

      response.status(upstream.status).send(Buffer.from(await upstream.arrayBuffer()));
    } catch (error) {
      logger.error('No fue posible conectar con el backend de Fluora.', { error });
      response.status(503).set('Cache-Control', 'no-store').json({ mensaje: 'La API no está disponible temporalmente' });
    }
  }
);

exports.tenantPage = onRequest(
  { region: 'us-central1', memory: '256MiB', timeoutSeconds: 15, maxInstances: 10 },
  async (request, response) => {
    const { apiBaseUrl, marketingOrigin } = runtimeConfig();
    const match = request.path.match(/^\/e\/([a-z0-9]+(?:-[a-z0-9]+)*)(?:\/.*)?$/i);
    const slug = match?.[1]?.toLowerCase();
    if (!slug) {
      response.status(404).set('Cache-Control', 'public, max-age=60').send(renderNotFound('solicitada', marketingOrigin));
      return;
    }

    try {
      const [apiResponse, template] = await Promise.all([
        fetch(`${apiBaseUrl}/publico/sitio/${encodeURIComponent(slug)}`, {
          headers: { Accept: 'application/json' },
          signal: AbortSignal.timeout(8000)
        }),
        loadCsrTemplate(marketingOrigin)
      ]);

      if (!apiResponse.ok) {
        response.status(apiResponse.status === 404 ? 404 : 502)
          .set('Cache-Control', 'public, max-age=60')
          .send(renderNotFound(slug, marketingOrigin));
        return;
      }

      const tenant = await apiResponse.json();
      if (!tenant?.publicado || !tenant?.nombreComercial) {
        response.status(404).set('Cache-Control', 'public, max-age=60').send(renderNotFound(slug, marketingOrigin));
        return;
      }

      let productCatalog = null;
      const productMatch = request.path.match(/^\/e\/[^/]+\/productos(?:\/([a-z0-9]+(?:-[a-z0-9]+)*))?\/?$/i);
      if (productMatch) {
        const productPath = productMatch[1] ? `/${encodeURIComponent(productMatch[1].toLowerCase())}` : '';
        const productResponse = await fetch(`${apiBaseUrl}/publico/productos/sitio/${encodeURIComponent(slug)}${productPath}`, {
          headers: { Accept: 'application/json' },
          signal: AbortSignal.timeout(8000)
        });
        if (!productResponse.ok) {
          response.status(productResponse.status === 404 ? 404 : 502)
            .set('Cache-Control', 'public, max-age=60')
            .send(renderNotFound(productMatch[1] || 'productos', marketingOrigin));
          return;
        }
        productCatalog = await productResponse.json();
      }

      response.status(200)
        .set('Content-Type', 'text/html; charset=utf-8')
        .set('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
        .send(renderTenantHtml(template, tenant, request.originalUrl || request.path, marketingOrigin, productCatalog));
    } catch (error) {
      logger.error('No fue posible renderizar el SEO del tenant.', { slug, error });
      response.status(503)
        .set('Retry-After', '60')
        .set('Cache-Control', 'no-store')
        .send(renderNotFound(slug, marketingOrigin));
    }
  }
);

exports.sitemap = onRequest(
  { region: 'us-central1', memory: '256MiB', timeoutSeconds: 15, maxInstances: 5 },
  createSitemapHandler({ config: runtimeConfig, log: logger })
);

exports.landingPage = onRequest(
 {region:'us-central1',memory:'256MiB',timeoutSeconds:15,maxInstances:10},
 async(request,response)=>{
  const {apiBaseUrl,marketingOrigin}=runtimeConfig();
  const appOrigin=appOriginParam.value().trim().replace(/\/+$/,'');
  const slug=request.path.replace(/^\//,'').replace(/\/$/,'');
  if(!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)){
   response.status(404).set('Cache-Control','no-store').send(renderNotFound('solicitada',marketingOrigin));return;
  }
  try{
   if(!appOrigin||!/^https:\/\//i.test(appOrigin))throw new Error('Origen de aplicación no configurado');
   const [data,template]=await Promise.all([
    fetch(apiBaseUrl+'/publico/marketing/landings/'+encodeURIComponent(slug),{signal:AbortSignal.timeout(8000)}),
    loadCsrTemplate(marketingOrigin)
   ]);
   if(data.status===404){response.status(404).set('Cache-Control','no-store').send(renderNotFound('solicitada',marketingOrigin));return;}
   if(!data.ok)throw new Error('API de marketing no disponible');
   const landing=await data.json();
   response.status(200).set('Cache-Control','public,max-age=60,s-maxage=300').send(renderLandingHtml(template,landing,marketingOrigin,appOrigin));
  }catch(error){
   logger.error('No fue posible cargar la landing',{error});
   response.status(503).set('Cache-Control','no-store').set('Retry-After','60').send('Página temporalmente no disponible');
  }
 }
);

exports.directoryPage = onRequest(
 {region:'us-central1',memory:'256MiB',timeoutSeconds:15,maxInstances:10},
 async(request,response)=>{
  const {apiBaseUrl,marketingOrigin}=runtimeConfig();
  try {
   const url=new URL(request.originalUrl || request.url,marketingOrigin);
   const params=new URLSearchParams();
   for(const key of ['q','ubicacion','categoria','cursor']){
    if(url.searchParams.has(key))params.set(key,url.searchParams.get(key));
   }
   const [upstream,template]=await Promise.all([
    fetch(`${apiBaseUrl}/publico/marketplace?${params}`,{headers:{Accept:'application/json'},signal:AbortSignal.timeout(8000)}),
    loadCsrTemplate(marketingOrigin)
   ]);
   if(!upstream.ok){response.status(upstream.status===400?400:503).set('Cache-Control','no-store').send(renderNotFound('directorio',marketingOrigin));return;}
   const directory=await upstream.json();
   response.status(200).set('Content-Type','text/html; charset=utf-8').set('Cache-Control','no-store')
    .send(renderDirectoryHtml(template,directory,marketingOrigin,params));
  }catch(error){
   logger.error('No fue posible renderizar el directorio.',{error});
   response.status(503).set('Retry-After','60').set('Cache-Control','no-store').send(renderNotFound('directorio',marketingOrigin));
  }
 }
);

// Isolated Hosting target for customer-owned domains. No registrar or Hosting writes.
const { createCustomDomainHandler } = require('./custom-domain-page');
exports.customDomainPage = onRequest(
  { region: 'us-central1', memory: '256MiB', timeoutSeconds: 30, maxInstances: 10 },
  createCustomDomainHandler({config: () => ({...runtimeConfig(), clientesOrigin: clientesOriginParam.value().trim()}), log: detail => logger.error('No fue posible renderizar el dominio propio.', detail)})
);
