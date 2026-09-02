import { createAppConfig } from './app.config.base';
import { routes } from './app.routes';

export { createAppConfig } from './app.config.base';

export const appConfig = createAppConfig(routes);
