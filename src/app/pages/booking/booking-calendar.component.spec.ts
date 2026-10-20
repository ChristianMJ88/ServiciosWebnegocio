import { AfterViewInit, Component, InjectionToken, forwardRef, inject, input } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FullCalendarComponent } from '@fullcalendar/angular';
import type { CalendarOptions } from '@fullcalendar/core';
import { vi } from 'vitest';
import { BookingCalendarComponent } from './booking-calendar.component';

const START_UNREADY = new InjectionToken<boolean>('Calendar starts without API');
@Component({
  selector: 'full-calendar', standalone: true, template: '<div>Calendario de prueba</div>',
  providers: [{ provide: FullCalendarComponent, useExisting: forwardRef(() => CalendarProbe) }]
})
class CalendarProbe implements AfterViewInit {
  readonly options = input<CalendarOptions>({});
  currentDate = new Date(2026, 0, 1);
  readonly instance = { getDate: () => this.currentDate, gotoDate: vi.fn() };
  private readonly startUnready = inject(START_UNREADY);
  api: typeof this.instance | null = null;
  ngAfterViewInit() { if (!this.startUnready) this.api = this.instance; }
  getApi() { return this.api; }
}

describe('BookingCalendar: fecha tras inicializar la vista', () => {
  async function create(startUnready = false) {
    await TestBed.configureTestingModule({
      imports: [BookingCalendarComponent], providers: [{ provide: START_UNREADY, useValue: startUnready }]
    }).overrideComponent(BookingCalendarComponent, { set: { imports: [CalendarProbe] } }).compileComponents();
    const fixture = TestBed.createComponent(BookingCalendarComponent);
    fixture.componentRef.setInput('options', { initialView: 'dayGridMonth' });
    fixture.componentRef.setInput('selectedDate', '2027-02-15');
    return fixture;
  }
  it('aplica la primera fecha tras ngAfterViewInit y conserva los cambios posteriores', async () => {
    const errors = vi.spyOn(console, 'error');
    const fixture = await create(); fixture.detectChanges(); await fixture.whenStable();
    const calendar = fixture.debugElement.query(By.directive(CalendarProbe)).componentInstance as CalendarProbe;
    expect(calendar.instance.gotoDate).toHaveBeenCalledWith('2027-02-15');
    fixture.componentRef.setInput('selectedDate', '2027-03-12'); fixture.detectChanges(); await fixture.whenStable();
    expect(calendar.instance.gotoDate).toHaveBeenLastCalledWith('2027-03-12');
    expect(errors).not.toHaveBeenCalled();
    errors.mockRestore();
  });
  it('tolera una API aún nula y actualiza cuando ya está disponible', async () => {
    const errors = vi.spyOn(console, 'error');
    const fixture = await create(true); fixture.detectChanges(); await fixture.whenStable();
    const calendar = fixture.debugElement.query(By.directive(CalendarProbe)).componentInstance as CalendarProbe;
    expect(calendar.instance.gotoDate).not.toHaveBeenCalled();
    calendar.api = calendar.instance;
    fixture.componentRef.setInput('selectedDate', '2027-03-12'); fixture.detectChanges(); await fixture.whenStable();
    expect(calendar.instance.gotoDate).toHaveBeenLastCalledWith('2027-03-12');
    expect(errors).not.toHaveBeenCalled();
    errors.mockRestore();
  });
  it('no vuelve a navegar cuando el calendario ya está en el día local solicitado', async () => {
    const fixture = await create(); fixture.detectChanges(); await fixture.whenStable();
    const calendar = fixture.debugElement.query(By.directive(CalendarProbe)).componentInstance as CalendarProbe;
    calendar.currentDate = new Date(2027, 2, 12, 23, 45);
    calendar.instance.gotoDate.mockClear();
    fixture.componentRef.setInput('selectedDate', '2027-03-12'); fixture.detectChanges(); await fixture.whenStable();
    expect(calendar.instance.gotoDate).not.toHaveBeenCalled();
  });
});
