import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PlatformAuthService } from '../../core/platform/platform-auth.service';

@Component({
  selector: 'app-platform-access', standalone: true, imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './platform-access.component.html', styleUrl: './platform-access.component.css'
})
export class PlatformAccessComponent {
  private readonly auth = inject(PlatformAuthService);
  private readonly router = inject(Router);
  correo = '';
  contrasena = '';
  readonly procesando = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    if (this.auth.autenticado()) void this.router.navigate(['/plataforma']);
  }

  ingresar(): void {
    if (!this.correo.trim() || !this.contrasena) return;
    this.procesando.set(true); this.error.set(null);
    this.auth.iniciar(this.correo, this.contrasena).pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: () => void this.router.navigate(['/plataforma']),
      error: error => this.error.set(error?.error?.message || error?.error?.detail || 'No fue posible iniciar sesión.')
    });
  }
}
