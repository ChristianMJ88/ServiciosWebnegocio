export interface FormularioCitaRecepcion {
  sucursalId: number | null;
  servicioId: number | null;
  clienteId: number | null;
  prestadorId: number | null;
  nombreCliente: string;
  correoCliente: string;
  telefonoCliente: string;
  fechaWalkIn: string;
  inicio: string;
  notas: string;
  avisarWhatsapp: boolean;
}
