import './app/marketing-locale';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { createMarketingAppConfig } from './app/app.config.marketing';
import { isHydratableMarketingHome, RuntimeWithFluoraOrigins } from './app/core/platform/platform-host.service';

bootstrapApplication(App,createMarketingAppConfig(isHydratableMarketingHome(globalThis.location,globalThis as RuntimeWithFluoraOrigins)))
  .catch((err) => console.error(err));
