import { provideEnvironmentInitializer } from '@angular/core';
import { prepareMarketingHome } from './app/core/marketing/marketing-bootstrap';
import { provideTenantPublicStyles } from './app/core/styles/tenant-public-styles';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import localeEsMx from '@angular/common/locales/es-MX';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { createAppConfig } from './app/app.config.base';
import { marketingRoutes } from './app/app.routes.marketing';

registerLocaleData(localeEs);
registerLocaleData(localeEsMx);

const config=createAppConfig(marketingRoutes);
bootstrapApplication(App,{...config,providers:[...config.providers,provideTenantPublicStyles(),provideEnvironmentInitializer(prepareMarketingHome)]})
  .catch((err) => console.error(err));
