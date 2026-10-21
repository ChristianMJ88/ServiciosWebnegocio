import './app/marketing-locale';
import { bootstrapApplication, BootstrapContext } from '@angular/platform-browser';
import { App } from './app/app';
import { marketingServerConfig } from './app/app.config.marketing.server';

export default (context: BootstrapContext) => bootstrapApplication(App,marketingServerConfig(),context);
