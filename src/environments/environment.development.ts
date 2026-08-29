import { resolveApiBaseUrl } from './api-base-url';

export const environment = {
  production: false,
  apiBaseUrl: resolveApiBaseUrl('/api/v1')
};
