import { Component, computed, effect, input, viewChild } from '@angular/core';
import { FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';
import type { CalendarOptions } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';

@Component({
  selector: 'app-booking-calendar',
  standalone: true,
  imports: [FullCalendarModule],
  template: '<full-calendar [options]="calendarOptions()"></full-calendar>'
})
export class BookingCalendarComponent {
  readonly options = input.required<CalendarOptions>();
  readonly selectedDate = input<string | null>(null);
  private readonly calendar = viewChild(FullCalendarComponent);
  constructor() {
    effect(() => {
      const fecha = this.selectedDate();
      const calendario = this.calendar();
      if (fecha && calendario) calendario.getApi().gotoDate(fecha);
    });
  }
  readonly calendarOptions = computed<CalendarOptions>(() => ({
    ...this.options(),
    plugins: [dayGridPlugin, interactionPlugin]
  }));
}
