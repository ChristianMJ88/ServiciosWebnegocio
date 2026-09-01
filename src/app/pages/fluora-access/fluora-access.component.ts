
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService, EmpresaAccesoApp } from '../../core/auth/auth.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';
import { OnboardingService } from '../../core/onboarding/onboarding.service';

@Component({
  selector: 'app-fluora-access',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './fluora-access.component.html',
  styleUrl: './fluora-access.component.css'
})
export class FluoraAccessComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly onboardingService = inject(OnboardingService);
  private readonly route = inject(ActivatedRoute);
  readonly platformHost = inject(PlatformHostService);

  readonly touched = signal(false);
  readonly loading = signal(false);
  readonly microsoftHabilitado = signal(false);
  readonly microsoftAccesoUrl = signal<string | null>(null);
  readonly googleHabilitado = signal(false);
  readonly googleAccesoUrl = signal<string | null>(null);
  readonly socialConfigReady = signal(false);
  readonly codigoAccesoSocial = signal<string | null>(null);
  empresas = signal<EmpresaAccesoApp[]>([]);
  error = '';
  readonly form = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', [Validators.required, Validators.minLength(8)]]
  });

  constructor() {
    if (this.platformHost.hasDedicatedAppHost() && !this.platformHost.isAppHost()) {
      globalThis.location.assign(this.platformHost.appUrl('/acceso'));
    }
  }

  ngOnInit(): void {
    this.onboardingService.configuracionRegistroSocial().subscribe({
      next: config => {
        this.microsoftHabilitado.set(config.microsoft);
        this.microsoftAccesoUrl.set(config.microsoftAccesoUrl);
        this.googleHabilitado.set(config.google);
        this.googleAccesoUrl.set(config.googleAccesoUrl);
        this.socialConfigReady.set(true);
      },
      error: () => this.socialConfigReady.set(true)
    });
    const errorSocial = this.route.snapshot.queryParamMap.get('errorSocial');
    if (errorSocial) this.error = errorSocial;
    const codigo = this.route.snapshot.queryParamMap.get('inicioSocial');
    if (codigo) {
      this.codigoAccesoSocial.set(codigo);
      this.intercambiarAccesoSocial(codigo);
    }
  }

  continuarConMicrosoft(): void {
    const ruta = this.microsoftAccesoUrl();
    if (!ruta || !this.microsoftHabilitado()) return;
    globalThis.location.assign(this.onboardingService.urlInicioSocial(ruta));
  }

  continuarConGoogle(): void {
    const ruta = this.googleAccesoUrl();
    if (!ruta || !this.googleHabilitado()) return;
    globalThis.location.assign(this.onboardingService.urlInicioSocial(ruta));
  }

  seleccionarEmpresa(empresaId: number): void {
    const codigo = this.codigoAccesoSocial();
    if (codigo) this.intercambiarAccesoSocial(codigo, empresaId);
    else this.iniciarSesion(empresaId);
  }

  private intercambiarAccesoSocial(codigo: string, empresaId?: number): void {
    this.loading.set(true);
    this.onboardingService.intercambiarAccesoSocial(codigo, empresaId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          if (response.estado === 'SELECCION_EMPRESA' && response.empresas?.length) {
            this.empresas.set(response.empresas);
            return;
          }
          if (!response.sesion) {
            this.error = 'El proveedor no devolvió una sesión válida.';
            return;
          }
          this.codigoAccesoSocial.set(null);
          this.empresas.set([]);
          this.authService.adoptarSesion(response.sesion, '');
          void this.router.navigateByUrl(this.authService.rutaPanelPersistida());
        },
        error: err => this.error = err?.error?.mensaje || err?.error?.detail || 'No se pudo iniciar sesión con la cuenta seleccionada.'
      });
  }

  iniciarSesion(empresaId?: number) {
    this.touched.set(true);
    if (this.form.invalid) {
      return;
    }

    this.loading.set(true);
    this.error = '';

    this.authService
      .appLogin({
        correo: this.form.value.correo ?? '',
        contrasena: this.form.value.contrasena ?? '',
        empresaId
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          if (response.estado === 'SELECCION_EMPRESA' && response.empresas?.length) {
            this.empresas.set(response.empresas);
            return;
          }

          this.empresas.set([]);
          void this.router.navigateByUrl(this.authService.rutaPanelPersistida());
        },
        error: err => {
          this.empresas.set([]);
          if (err?.error?.mensaje) {
            this.error = err.error.mensaje;
            return;
          }
          if (typeof err?.error === 'string' && err.error.trim()) {
            this.error = err.error;
            return;
          }
          this.error = err?.message || 'No se pudo iniciar sesión.';
        }
      });
  }
}
