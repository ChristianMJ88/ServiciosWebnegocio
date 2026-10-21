import { ApplicationConfig, mergeApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { createMarketingAppConfig } from './app.config.marketing';
import { marketingServerRoutes } from './app.routes.marketing.server';
import { MARKETING_RENDER_CONTEXT, parseProductionMarketingRenderContext } from './core/platform/marketing-render-context';

declare const FLUORA_RENDER_MARKETING_ORIGIN: string|undefined;
declare const FLUORA_RENDER_APP_ORIGIN: string|undefined;

export function createMarketingServerConfig(raw: unknown): ApplicationConfig {
  const context=parseProductionMarketingRenderContext(raw);
  return mergeApplicationConfig(createMarketingAppConfig(true),{providers:[
    provideServerRendering(withRoutes(marketingServerRoutes)),
    {provide:MARKETING_RENDER_CONTEXT,useValue:context}
  ]});
}

export function marketingServerConfig(): ApplicationConfig {
  return createMarketingServerConfig({route:'/',
    marketingOrigin:typeof FLUORA_RENDER_MARKETING_ORIGIN==='string'?FLUORA_RENDER_MARKETING_ORIGIN:undefined,
    appOrigin:typeof FLUORA_RENDER_APP_ORIGIN==='string'?FLUORA_RENDER_APP_ORIGIN:undefined
  });
}
