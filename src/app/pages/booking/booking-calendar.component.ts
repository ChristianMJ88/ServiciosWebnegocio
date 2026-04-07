import { Component, computed, input } from '@angular/core';
import { FullCalendarModule } from '@fullcalendar/angular';
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
  readonly calendarOptions = computed<CalendarOptions>(() => ({
    ...this.options(),
    plugins: [dayGridPlugin, interactionPlugin]
  }));
}
