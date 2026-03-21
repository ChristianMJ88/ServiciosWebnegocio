import { resolveApiBaseUrl } from './api-base-url';

export const environment = {
  production: true,
  empresaId: 1,
  apiBaseUrl: resolveApiBaseUrl(),
  useBackendCatalog: true,
  useBackendBooking: true,
  allowLegacyFallback: true,
  legacyScriptUrl:
    'https://script.google.com/macros/s/AKfycbx1VZhYuxQ8yMXv0jBJpnYTSuNbOKj9jwd_SXyi_u9eBqsNY17PwynCN6dRfoWFrH_BeA/exec',
  legacyToken: 'AGENDA2025'
};
