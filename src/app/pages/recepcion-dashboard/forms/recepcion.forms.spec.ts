import { describe, expect, it } from 'vitest';
import { crearFormularioCitaRecepcion } from './recepcion.forms';

describe('crearFormularioCitaRecepcion', () => {
  it('crea un formulario limpio con el contexto operativo recibido', () => {
    expect(crearFormularioCitaRecepcion('2026-08-25', 17)).toEqual({
      sucursalId: 17,
      servicioId: null,
      clienteId: null,
      prestadorId: null,
      nombreCliente: '',
      correoCliente: '',
      telefonoCliente: '',
      fechaWalkIn: '2026-08-25',
      inicio: '',
      notas: '',
      avisarWhatsapp: true
    });
  });
});
