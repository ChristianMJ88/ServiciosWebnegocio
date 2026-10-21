import { createMarketingAppConfig } from './app.config.marketing';

describe('contrato de configuración dedicada de marketing',()=>{
  it('solo agrega el proveedor de hidratación cuando el destino lo permite',()=>{
    expect(createMarketingAppConfig(true).providers.length).toBe(createMarketingAppConfig(false).providers.length+1);
  });
});
