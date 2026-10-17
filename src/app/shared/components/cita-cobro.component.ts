import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MoneyDisplayPipe } from '../pipes/money-display.pipe';

import { DatosCobroCita, ETIQUETAS_COBRO } from '../../core/caja/cobro-cita.models';

@Component({
  selector: 'app-cita-cobro', standalone: true, imports: [RouterLink, MoneyDisplayPipe],
  template: `
    @if (datos().estadoCobro; as estado) {
      <div class="cita-cobro" [class.cita-cobro--pendiente]="estado === 'PENDIENTE' || estado === 'PARCIAL'" role="status">
        <div class="cita-cobro__copy">
          <strong>{{ etiquetas[estado] || estado }}</strong>
          @if (estado === 'PENDIENTE' || estado === 'PARCIAL') {
            <span>Saldo {{ datos().pendiente | appMoney:datos().moneda:2:2 }}</span>
            @if (!datos().puedeCobrar) { <small>Queda saldo pendiente en Caja.</small> }
          }
          @if (estado === 'PAGADA' || estado === 'PARCIAL') {
            <small>Pagado {{ datos().pagado | appMoney:datos().moneda:2:2 }}</small>
          }
        </div>
        @if (datos().puedeCobrar === true) {
          <a class="cita-cobro__cta" [routerLink]="['/caja']" [queryParams]="{cita: citaId(), sucursal: datos().sucursalId}">
            Cobrar {{ datos().pendiente | appMoney:datos().moneda:2:2 }} <span aria-hidden="true">→</span>
          </a>
        }
      </div>
    }
  `,
  styles: [`
    :host { display:block; min-width:0; }
    .cita-cobro { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; padding:12px 14px; margin:12px 0; border:1px solid var(--mat-sys-outline-variant, #dce4e1); border-radius:14px; background:var(--mat-sys-surface-container-low, #f6f8f7); color:var(--mat-sys-on-surface, #263b35); }
    .cita-cobro--pendiente { border-inline-start:3px solid var(--mat-sys-primary, #426b5d); }
    .cita-cobro__copy { display:grid; gap:4px; }
    .cita-cobro__copy strong { font-size:.85rem; } .cita-cobro__copy small { opacity:.8; }
    .cita-cobro__cta { display:inline-flex; align-items:center; justify-content:center; gap:10px; min-height:44px; padding:10px 16px; border-radius:12px; text-decoration:none; font-weight:650; background:var(--mat-sys-primary, #426b5d); color:var(--mat-sys-on-primary, white); }
    .cita-cobro__cta:focus-visible { outline:3px solid currentColor; outline-offset:3px; }
    @media(max-width:575px) { .cita-cobro__cta { width:100%; } }
  `]
})
export class CitaCobroComponent {
  readonly datos = input.required<DatosCobroCita>();
  readonly citaId = input.required<number>();
  readonly etiquetas = ETIQUETAS_COBRO;
}
