import { CommonModule, DatePipe } from '@angular/common';
import { Component, Input, ViewEncapsulation } from '@angular/core';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';
import { CajaReceiptDto } from './caja-receipt.dto';

@Component({
  selector: 'app-caja-receipt',
  standalone: true,
  imports: [CommonModule, DatePipe, MoneyDisplayPipe],
  templateUrl: './caja-receipt.component.html',
  styleUrls: ['./caja-receipt.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class CajaReceiptComponent {
  @Input({ required: true }) receipt!: CajaReceiptDto | null;
}
