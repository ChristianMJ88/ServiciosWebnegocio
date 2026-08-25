import { CitaCliente } from '../../core/auth/client-appointments.service';

export interface RangoFechaAgenda {
  desde: string;
  hasta: string;
}

export interface CitaAgendaAgrupada extends CitaCliente {
  horaTexto: string;
  rangoTexto: string;
}

export interface GrupoFechaAgenda {
  fechaClave: string;
  fechaTexto: string;
  items: CitaAgendaAgrupada[];
}

export interface DistribucionAgenda {
  lane: number;
  laneCount: number;
}

export interface ResumenAgenda {
  total: number;
  pendientes: number;
  confirmadas: number;
  colaboradores: number;
}

export interface AnaliticaAgenda {
  ocupacion: number;
  horasReservadas: number;
  horasLibres: number;
}
