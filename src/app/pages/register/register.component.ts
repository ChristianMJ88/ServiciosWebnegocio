import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  readonly tenantContext = inject(TenantContextService);
  readonly platformHost = inject(PlatformHostService);
  readonly marketingMode = computed(() => !this.tenantContext.activo());
  readonly featureItems = computed(() => this.marketingMode()
    ? [
        {
          icon: 'bi-building-add',
          title: 'Acceso principal',
          body: 'Crea la cuenta inicial con la que después configurarás tu empresa dentro de Fluora.'
        },
        {
          icon: 'bi-diagram-3',
          title: 'Estructura por tenant',
          body: 'Usuarios globales, permisos por empresa y separación clara entre app y sitio público.'
        },
        {
          icon: 'bi-window-stack',
          title: 'Web incluida',
          body: 'Tu negocio obtiene su propia presencia digital para reservas, servicios y contacto.'
        }
      ]
    : [
        {
          icon: 'bi-lightning-charge',
          title: 'Registro simple',
          body: 'Completa tus datos en pocos pasos y deja listo tu acceso.'
        },
        {
          icon: 'bi-journal-check',
          title: 'Historial centralizado',
          body: 'Consulta tus próximas citas desde tu cuenta.'
        },
        {
          icon: 'bi-tablet-landscape',
          title: 'Acceso cómodo',
          body: 'Tu cuenta queda disponible en móvil, tablet y escritorio.'
        }
      ]);

  readonly loading = signal(false);
  readonly completed = signal(false);
  error = '';

  readonly form = this.fb.group({
    nombreCompleto: ['', [Validators.required, Validators.minLength(3)]],
    correo: ['', [Validators.required, Validators.email]],
    telefono: ['', [Validators.required, Validators.pattern('^[0-9+ ]{10,15}$')]],
    contrasena: ['', [Validators.required, Validators.minLength(8)]]
  });

  get f() {
    return this.form.controls;
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error = '';

    this.authService
      .register({
        nombreCompleto: this.form.value.nombreCompleto!.trim(),
        correo: this.form.value.correo!.trim(),
        telefono: this.form.value.telefono!.trim(),
        contrasena: this.form.value.contrasena!
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.completed.set(true);
          setTimeout(() => {
            globalThis.location.href = this.platformHost.appUrl('/acceso');
          }, 1200);
        },
        error: err => {
          this.error = err?.message || 'No se pudo completar el registro.';
        }
      });
  }
}
