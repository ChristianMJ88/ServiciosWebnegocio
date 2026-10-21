import { ApplicationConfig, EnvironmentProviders, mergeApplicationConfig, provideEnvironmentInitializer } from '@angular/core';
import { provideClientHydration, withEventReplay, withNoHttpTransferCache } from '@angular/platform-browser';
import { createAppConfig } from './app.config.base';
import { marketingRoutes } from './app.routes.marketing';
import { prepareMarketingHome } from './core/marketing/marketing-bootstrap';
import { provideTenantPublicStyles } from './core/styles/tenant-public-styles';

export function provideMarketingHydration(): EnvironmentProviders {
  return provideClientHydration(withEventReplay(), withNoHttpTransferCache());
}

/** Shared by the dedicated marketing client and future server probe, never by application. */
export function createMarketingAppConfig(hydrate: boolean): ApplicationConfig {
  return mergeApplicationConfig(createAppConfig(marketingRoutes), {providers:[
    provideTenantPublicStyles(), provideEnvironmentInitializer(prepareMarketingHome),
    ...(hydrate ? [provideMarketingHydration()] : [])
  ]});
}
