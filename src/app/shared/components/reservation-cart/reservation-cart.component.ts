import { Component, ElementRef, ViewChild, computed, input, output } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { ServicioCatalogo } from '../../../services/booking-data.service';
import { MoneyDisplayPipe } from '../../pipes/money-display.pipe';

@Component({
  selector: 'app-reservation-cart',
  standalone: true,
  imports: [NgTemplateOutlet, MoneyDisplayPipe],
  templateUrl: './reservation-cart.component.html',
  styleUrl: './reservation-cart.component.css'
})
export class ReservationCartComponent {
  readonly services = input<ServicioCatalogo[]>([]);
  readonly branch = input('');
  readonly date = input('');
  readonly time = input('');
  readonly actionLabel = input('Elegir fecha');
  readonly disabled = input(false);
  readonly remove = output<number>();
  readonly proceed = output<void>();
  @ViewChild('sheet') private sheet?: ElementRef<HTMLDialogElement>;
  readonly totals = computed(() => {
    const totals = new Map<string, number>();
    for (const service of this.services()) {
      const currency = service.moneda || 'MXN';
      totals.set(currency, (totals.get(currency) ?? 0) + service.precio);
    }
    return Array.from(totals, ([currency, amount]) => ({ currency, amount }));
  });
  readonly duration = computed(() => this.services().reduce((total, service) =>
    total + service.duracionMinutos + service.bufferAntesMinutos + service.bufferDespuesMinutos, 0));

  open(): void { this.sheet?.nativeElement.showModal(); }
  close(): void { this.sheet?.nativeElement.close(); }
  continue(): void { this.close(); this.proceed.emit(); }
}
