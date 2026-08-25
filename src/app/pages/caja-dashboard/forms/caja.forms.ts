import {
  AbrirCajaPayload,
  CerrarCajaPayload,
  RegistrarMovimientoCajaPayload,
  RegistrarPagoPayload
} from '../../../core/caja/caja.service';

export interface FormularioAperturaCaja { montoInicial: number; observaciones: string; }
export interface FormularioCierreCaja { montoContado: number; observaciones: string; }
export interface FormularioPagoCaja { monto: number; montoRecibido: number; metodoPago: string; referencia: string; observaciones: string; }
export interface FormularioMovimientoCaja { tipoMovimiento: string; monto: number; metodoPago: string; concepto: string; referencia: string; observaciones: string; }

export const crearFormularioApertura = (): FormularioAperturaCaja => ({ montoInicial: 0, observaciones: '' });
export const crearFormularioCierre = (): FormularioCierreCaja => ({ montoContado: 0, observaciones: '' });
export const crearFormularioPago = (): FormularioPagoCaja => ({ monto: 0, montoRecibido: 0, metodoPago: '', referencia: '', observaciones: '' });
export const crearFormularioMovimiento = (tipoMovimiento = '', metodoPago = ''): FormularioMovimientoCaja => ({
  tipoMovimiento, monto: 0, metodoPago, concepto: '', referencia: '', observaciones: ''
});

export const construirApertura = (sucursalId: number, formulario: FormularioAperturaCaja): AbrirCajaPayload => ({
  sucursalId,
  montoInicial: Number(formulario.montoInicial),
  observaciones: normalizar(formulario.observaciones)
});

export const construirCierre = (formulario: FormularioCierreCaja): CerrarCajaPayload => ({
  montoContado: Number(formulario.montoContado),
  observaciones: normalizar(formulario.observaciones)
});

export const construirPago = (formulario: FormularioPagoCaja): RegistrarPagoPayload => ({
  monto: Number(formulario.monto),
  metodoPago: formulario.metodoPago,
  referencia: normalizar(formulario.referencia),
  observaciones: normalizar(formulario.observaciones)
});

export const construirMovimiento = (sucursalId: number, formulario: FormularioMovimientoCaja): RegistrarMovimientoCajaPayload => ({
  sucursalId,
  tipoMovimiento: formulario.tipoMovimiento,
  monto: Number(formulario.monto),
  metodoPago: normalizar(formulario.metodoPago),
  concepto: formulario.concepto.trim(),
  referencia: normalizar(formulario.referencia),
  observaciones: normalizar(formulario.observaciones)
});

const normalizar = (valor: string): string | null => valor.trim() || null;
