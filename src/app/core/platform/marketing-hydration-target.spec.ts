import { isHydratableMarketingHome } from './platform-host.service';

describe('contrato del destino de hidratación de marketing', () => {
  const runtime={__FLUORA_MARKETING_ORIGIN__:'https://marketing.test',__FLUORA_APP_ORIGIN__:'https://app.test'};
  it.each([
    ['https://marketing.test/',true],['https://www.marketing.test/',true],
    ['https://marketing.test/?utm_source=fixture#planes',true],
    ['https://marketing.test/acceso',false],['https://marketing.test/privacidad',false],
    ['https://marketing.test/e/prueba',false],['https://marketing.test/negocios',false],
    ['https://marketing.test/barberias',false],['https://app.test/',false],
    ['https://reservas.negocio.test/',false]
  ])('solo activa el proveedor para el inicio de marketing: %s', (url,expected) => {
    expect(isHydratableMarketingHome(new URL(url),runtime)).toBe(expected);
  });
  it('conserva el desarrollo integrado usando la misma resolución de origen',()=>{
    expect(isHydratableMarketingHome(new URL('http://localhost:4200/'),{__FLUORA_MARKETING_ORIGIN__:'http://localhost:4200'})).toBe(true);
  });
  it('falla cerrado si runtime-config no aporta el origen de marketing',()=>{
    expect(isHydratableMarketingHome(new URL('https://reservas.negocio.test/'),{})).toBe(false);
    expect(isHydratableMarketingHome(new URL('https://marketing.test/'),{__FLUORA_MARKETING_ORIGIN__:'  '})).toBe(false);
  });
  it('no convierte un entorno sin location en un destino hidratable',()=>{
    expect(isHydratableMarketingHome(undefined,runtime)).toBe(false);
  });
});
