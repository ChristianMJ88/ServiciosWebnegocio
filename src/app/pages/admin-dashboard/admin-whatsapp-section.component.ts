import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import {
  AsociarChannelSenderWhatsappPayload,
  AuditoriaConfiguracionAdmin,
  ConfiguracionWhatsappAdmin,
  GuardarPlantillaWhatsappEmpresaPayload,
  GuardarConfiguracionWhatsappPayload,
  LogMensajeWhatsappAdmin,
  PlantillaWhatsappAdmin,
  PlantillaWhatsappEmpresaAdmin,
  ProbarPlantillaWhatsappPayload,
  ProvisionarMessagingServiceWhatsappPayload,
  ProvisionarSubcuentaWhatsappPayload
} from '../../core/admin/admin.service';
import { WhatsappChecklistItem, WhatsappOnboardingStats } from './admin-whatsapp.types';

@Component({
  selector: 'app-admin-whatsapp-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSlideToggleModule
  ],
  templateUrl: './admin-whatsapp-section.component.html',
  styleUrls: ['./admin-whatsapp-section.component.css']
})
export class AdminWhatsappSectionComponent {
  @Input({ required: true }) formularioWhatsapp!: GuardarConfiguracionWhatsappPayload;
  @Input({ required: true }) formularioProvisionSubcuentaWhatsapp!: ProvisionarSubcuentaWhatsappPayload;
  @Input({ required: true }) formularioProvisionMessagingServiceWhatsapp!: ProvisionarMessagingServiceWhatsappPayload;
  @Input({ required: true }) formularioAsociacionChannelSenderWhatsapp!: AsociarChannelSenderWhatsappPayload;
  @Input({ required: true }) formularioPruebaWhatsapp!: ProbarPlantillaWhatsappPayload;
  @Input({ required: true }) formularioPlantillaWhatsappEmpresa!: GuardarPlantillaWhatsappEmpresaPayload;
  @Input({ required: true }) guardandoWhatsapp!: boolean;
  @Input({ required: true }) guardandoPlantillaWhatsappEmpresa!: boolean;
  @Input({ required: true }) provisionandoSubcuentaWhatsapp!: boolean;
  @Input({ required: true }) provisionandoMessagingServiceWhatsapp!: boolean;
  @Input({ required: true }) asociandoChannelSenderWhatsapp!: boolean;
  @Input({ required: true }) detectandoChannelSenderWhatsapp!: boolean;
  @Input({ required: true }) probandoPlantillaWhatsapp!: boolean;
  @Input({ required: true }) plantillaWhatsappEmpresaEditandoId!: number | null;
  @Input({ required: true }) configuracionWhatsapp!: ConfiguracionWhatsappAdmin | null;
  @Input({ required: true }) whatsappOnboardingStats!: WhatsappOnboardingStats;
  @Input({ required: true }) whatsappOnboardingChecklist!: WhatsappChecklistItem[];
  @Input({ required: true }) auditoriaWhatsapp!: AuditoriaConfiguracionAdmin[];
  @Input({ required: true }) plantillasWhatsapp!: PlantillaWhatsappAdmin[];
  @Input({ required: true }) plantillasWhatsappEmpresa!: PlantillaWhatsappEmpresaAdmin[];
  @Input({ required: true }) logsWhatsapp!: LogMensajeWhatsappAdmin[];
  @Input({ required: true }) etiquetaAccionAuditoriaConfiguracion!: (accion: string) => string;

  @Output() save = new EventEmitter<void>();
  @Output() saveTemplateCatalog = new EventEmitter<void>();
  @Output() editTemplateCatalog = new EventEmitter<PlantillaWhatsappEmpresaAdmin>();
  @Output() cancelTemplateCatalogEdit = new EventEmitter<void>();
  @Output() deleteTemplateCatalog = new EventEmitter<PlantillaWhatsappEmpresaAdmin>();
  @Output() provisionSubaccount = new EventEmitter<void>();
  @Output() provisionMessagingService = new EventEmitter<void>();
  @Output() detectChannelSender = new EventEmitter<void>();
  @Output() associateChannelSender = new EventEmitter<void>();
  @Output() testTemplate = new EventEmitter<void>();
}
