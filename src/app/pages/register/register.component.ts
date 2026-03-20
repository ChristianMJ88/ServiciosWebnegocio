import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

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
          setTimeout(() => this.router.navigateByUrl('/login'), 1200);
        },
        error: err => {
          this.error = err?.message || 'No se pudo completar el registro.';
        }
      });
  }
}

