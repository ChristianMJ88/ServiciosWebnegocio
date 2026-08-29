import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
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
  ProvisionarSubcuentaWhatsappPayload,
  OnboardingWhatsappResponse,
  AdminService
} from '../../../core/admin/admin.service';
import { WhatsappChecklistItem, WhatsappOnboardingStats } from './admin-whatsapp.types';
import { MetaEmbeddedSignupService } from './meta-embedded-signup.service';

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
export class AdminWhatsappSectionComponent implements OnInit, OnDestroy {
  private readonly adminService = inject(AdminService);
  private readonly embeddedSignup = inject(MetaEmbeddedSignupService);

  vistaActiva: 'estado' | 'configuracion' | 'plantillas' | 'actividad' = 'estado';
  onboardingTechProvider: OnboardingWhatsappResponse | null = null;
  telefonoOnboarding = '';
  displayNameOnboarding = '';
  conectandoWhatsapp = false;
  errorOnboarding: string | null = null;
  private actualizacionOnboardingId: ReturnType<typeof setTimeout> | null = null;

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

  ngOnInit(): void {
    this.cargarEstadoOnboarding();
  }

  ngOnDestroy(): void {
    if (this.actualizacionOnboardingId) clearTimeout(this.actualizacionOnboardingId);
  }

  cargarEstadoOnboarding(): void {
    this.adminService.getEstadoOnboardingWhatsapp().subscribe({
      next: estado => {
        this.onboardingTechProvider = estado;
        this.telefonoOnboarding ||= estado.telefonoE164 ?? '';
        this.displayNameOnboarding ||= estado.displayName ?? '';
        this.programarActualizacion(estado);
      },
      error: () => this.errorOnboarding = 'No fue posible consultar el estado de conexión de WhatsApp.'
    });
  }

  async agregarWhatsapp(): Promise<void> {
    if (this.conectandoWhatsapp) return;
    this.errorOnboarding = null;
    this.conectandoWhatsapp = true;
    try {
      const inicio = await firstValueFrom(this.adminService.iniciarOnboardingWhatsapp({
          telefonoE164: this.telefonoOnboarding.trim(),
          displayName: this.displayNameOnboarding.trim()
        }));
      this.onboardingTechProvider = inicio;
      if (!inicio.metaAppId || !inicio.configurationId || !inicio.partnerSolutionId) {
        throw new Error('La plataforma todavía no tiene los identificadores de Embedded Signup.');
      }
      const meta = await this.embeddedSignup.abrir({
        appId: inicio.metaAppId,
        configurationId: inicio.configurationId,
        partnerSolutionId: inicio.partnerSolutionId
      });
      this.onboardingTechProvider = await firstValueFrom(this.adminService.completarOnboardingWhatsapp({
        onboardingId: inicio.onboardingId,
        wabaId: meta.wabaId,
        phoneNumberId: meta.phoneNumberId,
        telefonoE164: this.telefonoOnboarding.trim()
      }));
      this.programarActualizacion(this.onboardingTechProvider);
    } catch (error) {
      this.errorOnboarding = error instanceof Error ? error.message : 'No fue posible conectar WhatsApp.';
      this.cargarEstadoOnboarding();
    } finally {
      this.conectandoWhatsapp = false;
    }
  }

  estadoOnboardingActivo(): boolean {
    return this.onboardingTechProvider?.estado === 'ACTIVO';
  }

  async reintentarOnboarding(): Promise<void> {
    if (this.conectandoWhatsapp) return;
    this.conectandoWhatsapp = true;
    this.errorOnboarding = null;
    try {
      this.onboardingTechProvider = await firstValueFrom(this.adminService.reintentarOnboardingWhatsapp());
      this.programarActualizacion(this.onboardingTechProvider);
    } catch (error) {
      this.errorOnboarding = error instanceof Error ? error.message : 'No fue posible reintentar la configuración.';
    } finally {
      this.conectandoWhatsapp = false;
    }
  }

  private programarActualizacion(estado: OnboardingWhatsappResponse): void {
    if (this.actualizacionOnboardingId) clearTimeout(this.actualizacionOnboardingId);
    if (!['CONFIGURANDO', 'PENDIENTE_APROBACION'].includes(estado.estado)) return;
    this.actualizacionOnboardingId = setTimeout(() => this.cargarEstadoOnboarding(), 5000);
  }

  seleccionarVista(vista: 'estado' | 'configuracion' | 'plantillas' | 'actividad'): void {
    this.vistaActiva = vista;
  }
}
