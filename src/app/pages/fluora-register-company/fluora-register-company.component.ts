import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { OnboardingService, RegistrarEmpresaResponse } from '../../core/onboarding/onboarding.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({
  selector: 'app-fluora-register-company',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './fluora-register-company.component.html',
  styleUrl: './fluora-register-company.component.css'
})
export class FluoraRegisterCompanyComponent {
  private readonly fb = inject(FormBuilder);
  private readonly onboardingService = inject(OnboardingService);
  readonly platformHost = inject(PlatformHostService);

  readonly loading = signal(false);
  readonly submitted = signal<RegistrarEmpresaResponse | null>(null);
  readonly slugPreview = computed(() => this.slugify(this.form.value.slug || this.form.value.nombreEmpresa || 'tu-negocio'));
  error = '';

  readonly form = this.fb.group({
    nombreEmpresa: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
    slug: ['', [Validators.maxLength(100)]],
    giro: ['Salón de uñas y belleza', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    nombreAdministrador: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
    correoAdministrador: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefonoAdministrador: ['', [Validators.required, Validators.pattern('^[0-9+ ]{10,30}$')]],
    contrasena: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(120)]],
    zonaHoraria: ['America/Mexico_City', [Validators.required, Validators.maxLength(60)]]
  });

  readonly setupSteps = [
    'Se crea tu empresa y su dirección web inicial.',
    'Se genera el acceso principal para administrar el negocio.',
    'Fluora deja lista una sucursal principal y la web base de tu negocio.'
  ];

  readonly capabilities = [
    'Acceso central por correo y contraseña.',
    'Panel admin listo para configurar sucursales y servicios.',
    'Web pública inicial para empezar pruebas de tu negocio.'
  ];

  get f() {
    return this.form.controls;
  }

  cargarDemo() {
    const timestamp = Date.now().toString().slice(-6);
    this.form.patchValue({
      nombreEmpresa: `Studio Demo ${timestamp}`,
      slug: `studio-demo-${timestamp}`,
      giro: 'Salón de uñas y belleza',
      nombreAdministrador: 'Christian Mejía',
      correoAdministrador: `demo${timestamp}@fluora.local`,
      telefonoAdministrador: '5512345678',
      contrasena: 'Demo12345!',
      zonaHoraria: 'America/Mexico_City'
    });
    this.error = '';
    this.submitted.set(null);
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error = '';
    this.submitted.set(null);

    this.onboardingService.registrarEmpresa({
      nombreEmpresa: this.form.value.nombreEmpresa!.trim(),
      slug: (this.form.value.slug || '').trim() || undefined,
      giro: this.form.value.giro!.trim(),
      nombreAdministrador: this.form.value.nombreAdministrador!.trim(),
      correoAdministrador: this.form.value.correoAdministrador!.trim().toLowerCase(),
      telefonoAdministrador: this.form.value.telefonoAdministrador!.trim(),
      contrasena: this.form.value.contrasena!,
      zonaHoraria: this.form.value.zonaHoraria!.trim()
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          this.submitted.set(response);
        },
        error: err => {
          this.error = err?.error?.message || err?.error?.mensaje || err?.message || 'No se pudo crear la empresa.';
        }
      });
  }

  sitioUrl(): string {
    const response = this.submitted();
    return response ? this.platformHost.marketingUrl(response.rutaSitioPublico) : '#';
  }

  accesoUrl(): string {
    const response = this.submitted();
    return response ? this.platformHost.appUrl(response.rutaAcceso) : this.platformHost.appUrl('/acceso');
  }

  private slugify(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '') || 'tu-negocio';
  }
}
