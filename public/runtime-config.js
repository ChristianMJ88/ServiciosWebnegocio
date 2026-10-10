const esHostLocal = ['localhost', '127.0.0.1', '::1'].includes(window.location.hostname)
  || /^192\.168\.\d{1,3}\.\d{1,3}$/.test(window.location.hostname)
  || /^10\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(window.location.hostname)
  || /^172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3}$/.test(window.location.hostname);

// En desarrollo dejamos que api-base-url.ts resuelva el backend local.
// En hosts publicados conservamos la API de produccion como valor por defecto.
if (!window.__AGENDA_API_BASE_URL__ && !esHostLocal) {
  window.__AGENDA_API_BASE_URL__ = '/api/v1';
}
const origenLocalIntegrado = esHostLocal ? window.location.origin : null;
window.__FLUORA_MARKETING_ORIGIN__ = window.__FLUORA_MARKETING_ORIGIN__
  || origenLocalIntegrado
  || 'https://refluora.com';
window.__FLUORA_APP_ORIGIN__ = window.__FLUORA_APP_ORIGIN__
  || origenLocalIntegrado
  || 'https://app.refluora.com';

// Preparación de medición: sin IDs ficticios ni carga automática de proveedores.
// Habilitar únicamente junto con el consentimiento y la configuración real de GTM.
window.__FLUORA_MARKETING_TRACKING__ = window.__FLUORA_MARKETING_TRACKING__ || { enabled: false };
