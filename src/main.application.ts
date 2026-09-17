import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import localeEsMx from '@angular/common/locales/es-MX';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { createAppConfig } from './app/app.config.base';
import { applicationRoutes } from './app/app.routes.application';

registerLocaleData(localeEs);
registerLocaleData(localeEsMx);

bootstrapApplication(App, createAppConfig(applicationRoutes))
  .catch((err) => console.error(err));
