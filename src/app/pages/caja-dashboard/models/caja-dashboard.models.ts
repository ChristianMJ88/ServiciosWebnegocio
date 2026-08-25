import { CitaPorCobrar } from '../../../core/caja/caja.service';

export type VistaCaja = 'cobros' | 'sesion' | 'movimientos';

export interface CajaNotificationViewModel {
  titulo: string;
  descripcion: string;
  icono: string;
  total: number;
}

export interface CajaMetricViewModel {
  label: string;
  value: number;
  accent: 'primary' | 'neutral' | 'soft';
  format: 'money' | 'count';
}

export interface CajaViewOption {
  id: VistaCaja;
  label: string;
}

export type CajaActiveCharge = Pick<CitaPorCobrar, 'clienteNombre' | 'servicioNombre' | 'pendiente' | 'moneda'>;
