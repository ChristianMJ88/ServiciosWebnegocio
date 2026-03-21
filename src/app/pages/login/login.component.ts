import { Component, NgZone, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);

  readonly loading = signal(false);
  error = '';

  readonly form = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
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
      .login({
        correo: this.form.value.correo!.trim(),
        contrasena: this.form.value.contrasena!
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          const rutaDestino = this.authService.rutaPanelPersistida();
          this.ngZone.run(() => {
            void this.router.navigateByUrl(rutaDestino);
          });
        },
        error: err => {
          if (err?.error?.mensaje) {
            this.error = err.error.mensaje;
            return;
          }

          if (typeof err?.error === 'string' && err.error.trim()) {
            this.error = err.error;
            return;
          }

          if (err?.status >= 500 || err?.status === 0) {
            const hostActual = typeof globalThis !== 'undefined' && 'location' in globalThis
              ? globalThis.location.hostname
              : 'localhost';
            this.error = `El backend no está respondiendo correctamente. Verifica que el servidor API esté activo en http://${hostActual}:8080.`;
            return;
          }

          this.error = err?.message || 'No se pudo iniciar sesión.';
        }
      });
  }
}
