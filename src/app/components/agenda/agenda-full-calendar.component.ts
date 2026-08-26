import { Component, ViewEncapsulation, computed, effect, input, output, viewChild } from '@angular/core';
import { FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';
import type { CalendarOptions, EventClickArg, EventInput } from '@fullcalendar/core';
import esLocale from '@fullcalendar/core/locales/es';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin, { DateClickArg } from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import { AgendaCalendarView, toFullCalendarEvent, toFullCalendarView } from './agenda-full-calendar.helpers';
import { AgendaAppointmentVm, AgendaThemeMode } from './agenda.types';

@Component({
  selector: 'app-agenda-full-calendar',
  standalone: true,
  imports: [FullCalendarModule],
  templateUrl: './agenda-full-calendar.component.html',
  styleUrl: './agenda-full-calendar.component.css',
  encapsulation: ViewEncapsulation.None
})
export class AgendaFullCalendarComponent {
  readonly theme = input<AgendaThemeMode>('light');
  readonly viewMode = input<AgendaCalendarView>('week');
  readonly anchorDate = input('');
  readonly appointments = input<AgendaAppointmentVm[]>([]);

  readonly appointmentSelected = output<number>();
  readonly dateSelected = output<string>();

  private readonly calendar = viewChild(FullCalendarComponent);

  readonly events = computed<EventInput[]>(() => this.appointments().map(toFullCalendarEvent));

  readonly options = computed<CalendarOptions>(() => ({
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
    locale: esLocale,
    initialDate: this.anchorDate() || undefined,
    initialView: toFullCalendarView(this.viewMode()),
    headerToolbar: false,
    allDaySlot: false,
    nowIndicator: true,
    navLinks: true,
    selectable: true,
    selectMirror: true,
    dayMaxEvents: 3,
    moreLinkClick: 'popover',
    weekends: true,
    firstDay: 1,
    slotMinTime: '08:00:00',
    slotMaxTime: '21:00:00',
    slotDuration: '00:30:00',
    slotLabelInterval: '01:00:00',
    scrollTime: '08:00:00',
    expandRows: true,
    stickyHeaderDates: true,
    height: 'auto',
    contentHeight: 'auto',
    eventDisplay: 'block',
    eventTimeFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
    slotLabelFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
    dayHeaderFormat: this.viewMode() === 'month'
      ? { weekday: 'long' }
      : { weekday: 'short', day: 'numeric', month: 'short', omitCommas: true },
    events: this.events(),
    eventClick: (info: EventClickArg) => this.appointmentSelected.emit(Number(info.event.id)),
    dateClick: (info: DateClickArg) => this.dateSelected.emit(info.dateStr.slice(0, 10)),
    eventDidMount: info => {
      const collaborator = String(info.event.extendedProps['collaborator'] || 'Sin colaborador');
      const service = String(info.event.extendedProps['service'] || info.event.title);
      const status = String(info.event.extendedProps['statusLabel'] || '');
      info.el.title = [service, collaborator, status].filter(Boolean).join(' · ');
    }
  }));

  constructor() {
    effect(() => {
      const date = this.anchorDate();
      const view = toFullCalendarView(this.viewMode());
      const calendar = this.calendar();
      if (!calendar) {
        return;
      }
      queueMicrotask(() => {
        const api = calendar.getApi();
        if (api.view.type !== view) {
          api.changeView(view, date || undefined);
        } else if (date) {
          api.gotoDate(date);
        }
      });
    });
  }

}
