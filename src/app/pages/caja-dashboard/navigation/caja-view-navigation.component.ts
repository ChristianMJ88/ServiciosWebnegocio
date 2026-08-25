import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';
import { CajaActiveCharge, CajaViewOption, VistaCaja } from '../models/caja-dashboard.models';

@Component({
  selector: 'app-caja-view-navigation',
  standalone: true,
  imports: [MoneyDisplayPipe],
  templateUrl: './caja-view-navigation.component.html',
  styleUrls: ['./caja-view-navigation.component.css']
})
export class CajaViewNavigationComponent {
  @Input({ required: true }) vistas!: CajaViewOption[];
  @Input({ required: true }) vistaActiva!: VistaCaja;
  @Input({ required: true }) cobroActivo!: CajaActiveCharge | null;
  @Output() viewSelected = new EventEmitter<VistaCaja>();
}
