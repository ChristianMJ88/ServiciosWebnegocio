import { describe, expect, it } from 'vitest';
import { supportsMercadoPagoStatusScreen } from './mercado-pago-bricks.service';

describe('supportsMercadoPagoStatusScreen', () => {
  it('acepta identificadores numéricos de Payments API', () => {
    expect(supportsMercadoPagoStatusScreen('1234567890')).toBe(true);
  });

  it('rechaza referencias PAY de Orders API', () => {
    expect(supportsMercadoPagoStatusScreen('PAY01M3TNHEJEVA7TTCRM3P7SKZ3D')).toBe(false);
  });
});
