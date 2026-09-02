import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, Routes } from '@angular/router';
import { authInterceptor } from './core/auth/auth.interceptor';

export function createAppConfig(configuredRoutes: Routes): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideAnimationsAsync(),
      provideRouter(configuredRoutes),
      provideHttpClient(withInterceptors([authInterceptor])),
      { provide: LOCALE_ID, useValue: 'es' }
    ]
  };
}
