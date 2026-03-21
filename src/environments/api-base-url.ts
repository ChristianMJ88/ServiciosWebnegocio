type RuntimeWithAgendaApi = typeof globalThis & {
  __AGENDA_API_BASE_URL__?: string;
};

function limpiarUrl(url: string | null | undefined): string {
  return (url ?? '').trim().replace(/\/+$/, '');
}

function leerMetaApiBaseUrl(): string {
  if (typeof document === 'undefined') {
    return '';
  }

  const meta = document.querySelector('meta[name="agenda-api-base-url"]');
  return limpiarUrl(meta?.getAttribute('content'));
}

function esHostLocal(hostname: string): boolean {
  return hostname === 'localhost'
    || hostname === '127.0.0.1'
    || hostname === '::1'
    || /^192\.168\.\d{1,3}\.\d{1,3}$/.test(hostname)
    || /^10\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(hostname)
    || /^172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3}$/.test(hostname);
}

export function resolveApiBaseUrl(defaultValue = ''): string {
  const runtime = globalThis as RuntimeWithAgendaApi;
  const configuradaEnVentana = limpiarUrl(runtime.__AGENDA_API_BASE_URL__);
  if (configuradaEnVentana) {
    return configuradaEnVentana;
  }

  const configuradaEnMeta = leerMetaApiBaseUrl();
  if (configuradaEnMeta) {
    return configuradaEnMeta;
  }

  const hostname = globalThis.location?.hostname ?? '';
  if (esHostLocal(hostname)) {
    return `http://${hostname}:8080/api/v1`;
  }

  return limpiarUrl(defaultValue);
}
