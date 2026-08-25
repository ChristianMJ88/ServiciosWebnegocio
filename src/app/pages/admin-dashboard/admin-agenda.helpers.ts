import { getDurationMinutes } from '../../components/agenda/agenda.helpers';
import { AgendaViewMode } from '../../components/agenda/agenda.types';
import { CitaCliente } from '../../core/auth/client-appointments.service';
import {
  AnaliticaAgenda,
  DistribucionAgenda,
  GrupoFechaAgenda,
  RangoFechaAgenda,
  ResumenAgenda
} from './admin-agenda.types';

export function obtenerFechaLocalISO(fecha = new Date()): string {
  const year = fecha.getFullYear();
  const month = `${fecha.getMonth() + 1}`.padStart(2, '0');
  const day = `${fecha.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function sumarDiasAgenda(fechaIso: string, dias: number): string {
  const fecha = new Date(`${fechaIso}T12:00:00`);
  fecha.setDate(fecha.getDate() + dias);
  return obtenerFechaLocalISO(fecha);
}

export function obtenerRangoSemana(fechaIso: string): RangoFechaAgenda {
  const fecha = new Date(`${fechaIso}T12:00:00`);
  const ajusteInicio = fecha.getDay() === 0 ? -6 : 1 - fecha.getDay();
  const inicio = new Date(fecha);
  inicio.setDate(fecha.getDate() + ajusteInicio);
  const fin = new Date(inicio);
  fin.setDate(inicio.getDate() + 6);
  return { desde: obtenerFechaLocalISO(inicio), hasta: obtenerFechaLocalISO(fin) };
}

export function formatearFechaAgenda(fechaIso: string, view: AgendaViewMode): string {
  if (view === 'week') {
    const { desde, hasta } = obtenerRangoSemana(fechaIso);
    const inicio = new Date(`${desde}T12:00:00`);
    const fin = new Date(`${hasta}T12:00:00`);
    return `${inicio.toLocaleDateString('es-MX', { day: 'numeric', month: 'short' })} - ${fin.toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' })}`;
  }
  return new Date(`${fechaIso}T12:00:00`).toLocaleDateString('es-MX', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  });
}

export function agruparCitasPorFecha(citas: CitaCliente[]): GrupoFechaAgenda[] {
  const grupos = new Map<string, GrupoFechaAgenda>();
  for (const cita of [...citas].sort((a, b) => Date.parse(a.inicio) - Date.parse(b.inicio))) {
    const inicio = new Date(cita.inicio);
    const fin = new Date(cita.fin);
    const fechaClave = cita.inicio.slice(0, 10);
    const formatoHora = { hour: '2-digit', minute: '2-digit' } as const;
    const item = {
      ...cita,
      horaTexto: inicio.toLocaleTimeString('es-MX', formatoHora),
      rangoTexto: `${inicio.toLocaleTimeString('es-MX', formatoHora)} - ${fin.toLocaleTimeString('es-MX', formatoHora)}`
    };
    const grupo = grupos.get(fechaClave);
    if (grupo) {
      grupo.items.push(item);
    } else {
      grupos.set(fechaClave, {
        fechaClave,
        fechaTexto: inicio.toLocaleDateString('es-MX', { weekday: 'long', day: 'numeric', month: 'long' }),
        items: [item]
      });
    }
  }
  return Array.from(grupos.values());
}

export function filtrarCitasPeriodo(citas: CitaCliente[], fecha: string, view: AgendaViewMode): CitaCliente[] {
  const rango = view === 'week' ? obtenerRangoSemana(fecha) : { desde: fecha, hasta: fecha };
  return [...citas]
    .filter(cita => cita.inicio.slice(0, 10) >= rango.desde && cita.inicio.slice(0, 10) <= rango.hasta)
    .sort((a, b) => Date.parse(a.inicio) - Date.parse(b.inicio));
}

export function resumirAgenda(citas: CitaCliente[]): ResumenAgenda {
  return {
    total: citas.length,
    pendientes: citas.filter(cita => cita.estado === 'PENDIENTE').length,
    confirmadas: citas.filter(cita => cita.estado === 'CONFIRMADA').length,
    colaboradores: new Set(citas.map(cita => cita.prestadorId)).size
  };
}

export function calcularAnaliticaAgenda(
  citas: CitaCliente[],
  totalColaboradores: number,
  horasDisponibles: number,
  view: AgendaViewMode
): AnaliticaAgenda {
  const minutosReservados = citas.reduce((total, cita) => total + getDurationMinutes(cita.inicio, cita.fin), 0);
  const capacidadMinutos = Math.max(totalColaboradores, 1) * horasDisponibles * 60 * (view === 'week' ? 7 : 1);
  return {
    ocupacion: capacidadMinutos ? Math.min(100, Math.round((minutosReservados / capacidadMinutos) * 100)) : 0,
    horasReservadas: Math.max(0, Math.round((minutosReservados / 60) * 10) / 10),
    horasLibres: Math.max(0, Math.round(((capacidadMinutos - minutosReservados) / 60) * 10) / 10)
  };
}

export function calcularDistribucionAgenda<T extends { inicio: string; fin: string }>(
  eventos: T[]
): Map<T, DistribucionAgenda> {
  const ordenados = [...eventos].sort((a, b) => Date.parse(a.inicio) - Date.parse(b.inicio));
  const distribucion = new Map<T, DistribucionAgenda>();
  const finPorCarril: number[] = [];
  for (const evento of ordenados) {
    const inicio = Date.parse(evento.inicio);
    const fin = Date.parse(evento.fin);
    let lane = finPorCarril.findIndex(finCarril => finCarril <= inicio);
    if (lane === -1) {
      lane = finPorCarril.length;
      finPorCarril.push(fin);
    } else {
      finPorCarril[lane] = fin;
    }
    const laneCount = Math.max(1, ordenados.filter(candidato =>
      inicio < Date.parse(candidato.fin) && fin > Date.parse(candidato.inicio)
    ).length);
    distribucion.set(evento, { lane, laneCount });
  }
  return distribucion;
}
