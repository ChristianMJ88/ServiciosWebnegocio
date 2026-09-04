import { isMarketingHostname } from './platform-host.service';

describe('PlatformHostService host classification', () => {
  it('recognizes the configured marketing hostname', () => {
    expect(isMarketingHostname('refluora.com', 'refluora.com')).toBe(true);
  });

  it('recognizes the www alias of the configured marketing hostname', () => {
    expect(isMarketingHostname('www.refluora.com', 'refluora.com')).toBe(true);
  });

  it('does not classify tenant or application hosts as marketing', () => {
    expect(isMarketingHostname('app.refluora.com', 'refluora.com')).toBe(false);
    expect(isMarketingHostname('reservas.negocio.mx', 'refluora.com')).toBe(false);
  });
});
