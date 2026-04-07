import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import {
  AuditoriaConfiguracionAdmin,
  ConfiguracionCorreoAdmin,
  GuardarConfiguracionCorreoPayload
} from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-email-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatOptionModule,
    MatSelectModule,
    MatSlideToggleModule
  ],
  templateUrl: './admin-email-section.component.html'
})
export class AdminEmailSectionComponent {
  readonly formularioCorreo = input.required<GuardarConfiguracionCorreoPayload>();
  readonly guardandoCorreo = input(false);
  readonly configuracionCorreo = input<ConfiguracionCorreoAdmin | null>(null);
  readonly migrandoSecretosCorreo = input(false);
  readonly auditoriaCorreo = input<AuditoriaConfiguracionAdmin[]>([]);
  readonly etiquetaAccionAuditoriaConfiguracion = input.required<(accion: string) => string>();

  readonly save = output<void>();
  readonly migrateSecrets = output<void>();
}
