
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService, EmpresaAccesoApp } from '../../core/auth/auth.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({
  selector: 'app-fluora-access',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './fluora-access.component.html',
  styleUrl: './fluora-access.component.css'
})
export class FluoraAccessComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  readonly platformHost = inject(PlatformHostService);

  readonly touched = signal(false);
  readonly loading = signal(false);
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
