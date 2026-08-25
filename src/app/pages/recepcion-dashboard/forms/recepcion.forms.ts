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

export function crearFormularioCitaRecepcion(
  fechaWalkIn: string,
  sucursalId: number | null = null
): FormularioCitaRecepcion {
  return {
    sucursalId,
    servicioId: null,
    clienteId: null,
    prestadorId: null,
    nombreCliente: '',
    correoCliente: '',
    telefonoCliente: '',
    fechaWalkIn,
    inicio: '',
    notas: '',
    avisarWhatsapp: true
  };
}
