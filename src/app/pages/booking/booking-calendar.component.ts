import { Component, afterRenderEffect, computed, input, viewChild } from '@angular/core';
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
    afterRenderEffect({ write: () => {
      const fecha = this.selectedDate();
      const api = this.calendar()?.getApi();
      if (!fecha || !api) return;
      const actual = api.getDate();
      const fechaLocal = `${actual.getFullYear()}-${String(actual.getMonth() + 1).padStart(2, '0')}-${String(actual.getDate()).padStart(2, '0')}`;
      if (fechaLocal !== fecha) api.gotoDate(fecha);
    }});
  }
  readonly calendarOptions = computed<CalendarOptions>(() => ({
    ...this.options(),
    plugins: [dayGridPlugin, interactionPlugin]
  }));
}
