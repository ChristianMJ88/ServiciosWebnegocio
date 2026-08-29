import { resolveApiBaseUrl } from './api-base-url';

export const environment = {
  production: true,
  apiBaseUrl: resolveApiBaseUrl()
};
