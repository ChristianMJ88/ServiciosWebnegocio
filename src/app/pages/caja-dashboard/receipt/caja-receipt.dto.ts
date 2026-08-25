export interface CajaReceiptPaymentDto {
  fecha: string;
  metodo: string;
  referencia: string;
  monto: number;
}

export interface CajaReceiptDto {
  empresa: string;
  cliente: string;
  sucursal: string;
  servicio: string;
  inicioCita: string;
  moneda: string;
  total: number;
  totalPagado: number;
  pendiente: number;
  pagos: CajaReceiptPaymentDto[];
}
