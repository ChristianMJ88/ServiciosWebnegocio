export interface DatosCobroCita {
  estadoCobro?: string;
  cobroPointEnCurso?: boolean;
  pagado?: number;
  pendiente?: number;
  condonado?: number;
  puedeCobrar?: boolean;
  moneda?: string;
  sucursalId?: number;
}
export interface ResultadoCierreCita extends DatosCobroCita { citaId: number; estado: string; }

export const ETIQUETAS_COBRO: Record<string,string> = {SIN_COSTO:'Sin costo', PAGADA:'Pagada', PARCIAL:'Pago parcial', PENDIENTE:'Pendiente de pago', CONDONADA:'Cerrada sin cobro', NO_COBRABLE:'Sin saldo cobrable'};
