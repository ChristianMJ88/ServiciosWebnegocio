import { Component, input, output } from '@angular/core';
import { EstadoOnboarding } from '../../../core/onboarding/onboarding.service';

@Component({
  selector: 'app-admin-onboarding-card',
  standalone: true,
  templateUrl: './admin-onboarding-card.component.html',
  styleUrl: './admin-onboarding-card.component.css'
})
export class AdminOnboardingCardComponent {
  readonly estado = input.required<EstadoOnboarding>();
  readonly reenviando = input(false);
  readonly abrirPaso = output<string>();
  readonly reenviar = output<void>();
}
