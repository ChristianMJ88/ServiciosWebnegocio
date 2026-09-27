import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PublicCheckoutConfig {
  habilitado: boolean;
  proveedor: string;
  ambiente: 'PRUEBA' | 'PRODUCCION';
  llavePublica: string | null;
  aceptarTarjeta: boolean;
  aceptarTransferencia: boolean;
  aceptarEfectivoReferenciado: boolean;
  permitirPagoProductos: boolean;
  permitirPagoServicios: boolean;
  permitirRecoleccion: boolean;
  permitirEntregaLocal: boolean;
  costoEntregaLocal: number;
  permitirEnvioNacional: boolean;
  costoEnvioNacional: number;
  modalidadServicios: string;
  anticipoPorcentaje: number | null;
  anticipoImporte: number | null;
  moneda: string;
}

export interface CheckoutOrder {
  id: string;
  estado: string;
  sucursalId: number;
  moneda: string;
  subtotal: number;
  descuentoTotal: number;
  impuestosTotal: number;
  costoEnvio: number;
  total: number;
  modalidadEntrega: DeliveryMode;
  expiraEn: string;
  detalles: Array<{
    tipo: string;
    productoId: number | null;
    servicioId: number | null;
    citaId: number | null;
    nombre: string;
    cantidad: number;
    precioListaUnitario: number;
    precioUnitario: number;
    descuento: number;
    impuestos: number;
    total: number;
  }>;
}

export type DeliveryMode = 'RECOGE_EN_SUCURSAL' | 'ENTREGA_LOCAL' | 'ENVIO_NACIONAL';

export interface DeliveryAddressInput {
  calle: string;
  numeroExterior: string;
  numeroInterior: string | null;
  colonia: string;
  estado: string;
  municipio: string;
  codigoPostal: string;
  referencias: string | null;
}

export interface CheckoutPayment {
  ordenId: string;
  estadoOrden: string;
  estadoPago: string;
  proveedor: string;
  referenciaExterna: string | null;
  importe: number;
  moneda: string;
  accionUrl: string | null;
  codigoRespuesta: string | null;
  detalleRespuesta: string | null;
}

export interface CardBrickData {
  token: string;
  payment_method_id: string;
  payment_type_id?: string;
  installments: number;
  payer?: { email?: string };
}

@Injectable({ providedIn: 'root' })
export class PublicCheckoutService {
  private readonly http = inject(HttpClient);

  config(slug: string): Observable<PublicCheckoutConfig> {
    return this.http.get<PublicCheckoutConfig>(
      `${environment.apiBaseUrl}/publico/empresas/${encodeURIComponent(slug)}/checkout/configuracion`
    );
  }

  createOrder(slug: string, input: {
    sucursalId: number;
    zonaPrecioClave: string | null;
    correoCliente: string;
    nombreDestinatario: string;
    telefonoDestinatario: string;
    modalidadEntrega: DeliveryMode;
    direccion: DeliveryAddressInput | null;
    productos: Array<{ productoId: number; cantidad: number }>;
    citaIds: number[];
  }, idempotencyKey: string): Observable<CheckoutOrder> {
    return this.http.post<CheckoutOrder>(
      `${environment.apiBaseUrl}/publico/empresas/${encodeURIComponent(slug)}/checkout/ordenes`,
      input,
      { headers: this.idempotency(idempotencyKey) }
    );
  }

  payCard(slug: string, orderId: string, data: CardBrickData, email: string,
          idempotencyKey: string): Observable<CheckoutPayment> {
    return this.http.post<CheckoutPayment>(
      `${environment.apiBaseUrl}/publico/empresas/${encodeURIComponent(slug)}/checkout/ordenes/${encodeURIComponent(orderId)}/pagos/tarjeta`,
      {
        tokenMedioPago: data.token,
        metodoPagoId: data.payment_method_id,
        tipoPagoId: data.payment_type_id,
        parcialidades: data.installments,
        correoPagador: email
      },
      { headers: this.idempotency(idempotencyKey) }
    );
  }

  private idempotency(value: string): HttpHeaders {
    return new HttpHeaders({ 'Idempotency-Key': value });
  }
}
