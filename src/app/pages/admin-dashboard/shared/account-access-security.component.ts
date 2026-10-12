import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { AccountAccessSecurityService } from '../../../core/admin/account-access-security.service';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-account-access-security',
  standalone: true,
  imports: [FormsModule, MatButtonModule],
  template: `
    @if (bloqueado) { <span class="blocked">Acceso bloqueado</span> }
    <button mat-stroked-button type="button" (click)="abierto.set(!abierto())">{{ bloqueado ? 'Desbloquear acceso' : 'Bloquear acceso' }}</button>
    @if (abierto()) {
      <div class="confirmation">
        <p>{{ bloqueado ? 'Desbloquear no reactiva una cuenta inactiva. Deberá iniciar sesión de nuevo.' : 'Se cerrará el acceso de sus sesiones. Sus citas e historial se conservarán.' }}</p>
        <label>Motivo<textarea [(ngModel)]="motivo" maxlength="500" rows="3" placeholder="Describe la causa de esta decisión"></textarea></label>
        @if (error()) { <p role="alert">{{ error() }}</p> }
        <div class="actions">
          <button mat-button type="button" [disabled]="guardando()" (click)="abierto.set(false)">Cancelar</button>
          <button mat-flat-button type="button" [disabled]="guardando() || !motivo.trim()" (click)="confirmar()">{{ guardando() ? 'Guardando…' : 'Confirmar' }}</button>
        </div>
      </div>
    }
  `,
  styles: [`:host{display:flex;flex-direction:column;gap:8px;min-width:0}.blocked{color:#a32232;font-weight:600}.confirmation{padding:14px;border:1px solid #dce1ea;border-radius:14px;background:#f8f9fc;max-width:360px;text-align:left}p{font-size:13px;line-height:1.5;margin:0 0 10px}label{display:grid;gap:8px}textarea{width:100%;box-sizing:border-box;font:inherit;font-size:16px;border:1px solid #ccd3df;border-radius:8px;padding:10px}.actions{display:flex;justify-content:flex-end;gap:8px;margin-top:12px}button{min-height:44px;white-space:normal}`]
})
export class AccountAccessSecurityComponent {
  @Input({ required: true }) usuarioId!: number;
  @Input() bloqueado = false;
  @Input() prestador = false;
  @Output() changed = new EventEmitter<void>();
  private readonly seguridad = inject(AccountAccessSecurityService);
  readonly abierto = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  motivo = '';

  confirmar() {
    if (this.guardando() || !this.motivo.trim()) return;
    this.guardando.set(true); this.error.set('');
    this.seguridad.cambiar(this.usuarioId, this.prestador, !this.bloqueado, this.motivo.trim())
      .pipe(finalize(() => this.guardando.set(false))).subscribe({
        next: () => { this.abierto.set(false); this.motivo = ''; this.changed.emit(); },
        error: error => this.error.set(error.error?.detail || error.error?.message || 'No se pudo cambiar el acceso.')
      });
  }
}
