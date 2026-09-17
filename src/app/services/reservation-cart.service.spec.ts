import { ReservationCartService } from './reservation-cart.service';
import { ServicioCatalogo } from './booking-data.service';
import { MoneyDisplayPipe } from '../shared/pipes/money-display.pipe';

describe('Reservation draft boundaries', () => {
  beforeEach(() => { const cart = new ReservationCartService(); cart.clear(1); cart.clear(2); });
  it('keeps identical service IDs in different tenants isolated', () => {
    const cart = new ReservationCartService();
    cart.save(1, 10, [{ id: 7 } as ServicioCatalogo]);
    cart.save(2, 20, [{ id: 7 }, { id: 8 }] as ServicioCatalogo[]);
    cart.clear(1);
    expect(cart.read(1)).toBeUndefined();
    expect(cart.read(2)).toEqual({ branchId: 20, serviceIds: [7, 8] });
    expect(cart.read(null)).toBeUndefined();
  });

  it('restores IDs after reload without mixing tenants', () => {
    new ReservationCartService().save(1, 10, [{ id: 7 }] as ServicioCatalogo[]);
    expect(new ReservationCartService().read(1)).toEqual({ branchId: 10, serviceIds: [7] });
    expect(new ReservationCartService().read(2)).toBeUndefined();
  });

  it('stores identifiers rather than stale prices and protects saved selection from mutation', () => {
    const cart = new ReservationCartService();
    cart.save(1, 10, [{ id: 7, precio: 100 }] as ServicioCatalogo[]);
    cart.read(1)!.serviceIds.push(9);
    expect(cart.read(1)).toEqual({ branchId: 10, serviceIds: [7] });
  });
});

describe('Mexican reservation prices', () => {
  it('puts the currency symbol before the amount and always shows cents', () => {
    const pipe = new MoneyDisplayPipe();
    expect(pipe.transform(1234.5, 'MXN')).toBe('$1,234.50');
    expect(pipe.transform(250, 'MXN')).toBe('$250.00');
    expect(pipe.transform(250.75, 'MXN')).toBe('$250.75');
    expect(pipe.transform(250, 'USD')).toContain('USD');
  });
});
