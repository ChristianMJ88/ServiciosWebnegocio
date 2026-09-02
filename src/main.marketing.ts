import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { createAppConfig } from './app/app.config.base';
import { marketingRoutes } from './app/app.routes.marketing';

registerLocaleData(localeEs);

bootstrapApplication(App, createAppConfig(marketingRoutes))
  .catch((err) => console.error(err));
