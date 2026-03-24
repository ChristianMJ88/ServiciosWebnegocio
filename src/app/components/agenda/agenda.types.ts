export type AgendaThemeMode = 'dark' | 'light';

export interface AgendaStatCardVm {
  label: string;
  value: string | number;
}

export interface AgendaOccupancyVm {
  percent: number;
  bookedLabel: string;
  freeLabel: string;
  helper?: string;
}

export interface AgendaCollaboratorVm {
  id: number | string;
  name: string;
  meta: string;
  avatar: string;
  accentColor?: string | null;
}

export interface AgendaDetailFieldVm {
  label: string;
  value: string;
}

export interface AgendaActionVm {
  id: string;
  label: string;
  kind?: 'primary' | 'secondary' | 'ghost' | 'danger';
  disabled?: boolean;
  externalUrl?: string | null;
}

export interface AgendaAppointmentVm {
  id: number;
  status: string;
  statusLabel: string;
  title: string;
  subtitle: string;
  supportingText?: string;
  supportingTextSecondary?: string;
  priceLabel?: string;
  avatarLabel: string;
  top: number;
  height: number;
  leftPct: number;
  widthPct: number;
  start: string;
  end: string;
  detailTitle: string;
  detailEyebrow?: string;
  notes?: string | null;
  metaFields: AgendaDetailFieldVm[];
  actions: AgendaActionVm[];
}
