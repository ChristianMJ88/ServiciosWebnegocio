
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { OnboardingService, RegistrarEmpresaResponse } from '../../core/onboarding/onboarding.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({
  selector: 'app-fluora-register-company',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './fluora-register-company.component.html',
  styleUrl: './fluora-register-company.component.css'
})
export class FluoraRegisterCompanyComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly onboardingService = inject(OnboardingService);
  private readonly route = inject(ActivatedRoute);
  readonly platformHost = inject(PlatformHostService);

  readonly loading = signal(false);
  readonly submitted = signal<RegistrarEmpresaResponse | null>(null);
  readonly socialLoading = signal(false);
  readonly socialToken = signal<string | null>(null);
  readonly socialProvider = signal<string | null>(null);
  readonly socialConfig = signal({google: false, microsoft: false, apple: false, googleInicioUrl: null as string | null});
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

  ngOnInit(): void {
    this.onboardingService.configuracionRegistroSocial().subscribe({
      next: config => this.socialConfig.set(config),
      error: () => this.socialConfig.set({google: false, microsoft: false, apple: false, googleInicioUrl: null})
    });

    const errorSocial = this.route.snapshot.queryParamMap.get('errorSocial');
    if (errorSocial) this.error = errorSocial;
    const token = this.route.snapshot.queryParamMap.get('registroSocial');
    if (token) this.cargarPerfilSocial(token);
  }

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

  continuarConGoogle(): void {
    const ruta = this.socialConfig().googleInicioUrl;
    if (!ruta || !this.socialConfig().google) return;
    window.location.assign(this.onboardingService.urlInicioSocial(ruta));
  }

  private cargarPerfilSocial(token: string): void {
    this.socialLoading.set(true);
    this.onboardingService.perfilRegistroSocial(token)
      .pipe(finalize(() => this.socialLoading.set(false)))
      .subscribe({
        next: perfil => {
          this.socialToken.set(token);
          this.socialProvider.set(perfil.proveedor);
          this.form.patchValue({
            nombreAdministrador: perfil.nombre,
            correoAdministrador: perfil.correo
          });
          this.form.controls.correoAdministrador.disable();
          this.error = '';
        },
        error: err => {
          this.error = err?.error?.message || 'La autorización social expiró. Inténtalo nuevamente.';
        }
      });
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error = '';
    this.submitted.set(null);

    const valores = this.form.getRawValue();
    this.onboardingService.registrarEmpresa({
      nombreEmpresa: valores.nombreEmpresa!.trim(),
      slug: (valores.slug || '').trim() || undefined,
      giro: valores.giro!.trim(),
      nombreAdministrador: valores.nombreAdministrador!.trim(),
      correoAdministrador: valores.correoAdministrador!.trim().toLowerCase(),
      telefonoAdministrador: valores.telefonoAdministrador!.trim(),
      contrasena: valores.contrasena!,
      zonaHoraria: valores.zonaHoraria!.trim(),
      registroSocialToken: this.socialToken() || undefined
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
