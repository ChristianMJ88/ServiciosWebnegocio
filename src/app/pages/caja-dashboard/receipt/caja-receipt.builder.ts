import { CitaPorCobrar, OpcionCaja, PagoCita } from '../../../core/caja/caja.service';
import { CajaReceiptDto } from './caja-receipt.dto';

export function construirComprobanteCaja(
  empresa: string,
  cita: CitaPorCobrar,
  pagos: PagoCita[],
  metodosPago: OpcionCaja[]
): CajaReceiptDto {
  const total = numeroSeguro(cita.total);

  return {
    empresa,
    cliente: cita.clienteNombre,
    sucursal: cita.sucursalNombre,
    servicio: cita.servicioNombre,
    inicioCita: cita.inicio,
    moneda: cita.moneda,
    total,
    totalPagado: numeroSeguro(cita.pagado),
    pendiente: numeroSeguro(cita.pendiente),
    pagos: pagos.map(pago => ({
      fecha: pago.registradoEn,
      metodo: metodosPago.find(metodo => metodo.codigo === pago.metodoPago)?.etiqueta ?? pago.metodoPago,
      referencia: pago.referencia?.trim() || '-',
      monto: numeroSeguro(pago.monto)
    }))
  };
}

function numeroSeguro(valor: number): number {
  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : 0;
}
