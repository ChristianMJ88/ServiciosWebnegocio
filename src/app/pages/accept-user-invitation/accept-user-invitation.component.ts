import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { UserInvitationDetails, UserInvitationService } from '../../core/invitations/user-invitation.service';

@Component({
  selector: 'app-accept-user-invitation',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './accept-user-invitation.component.html',
  styleUrl: './accept-user-invitation.component.css'
})
export class AcceptUserInvitationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly invitations = inject(UserInvitationService);
  readonly details = signal<UserInvitationDetails | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly accepted = signal(false);
  error = '';
  token = '';
  readonly form = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(100),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{10,100}$/)]],
    confirmation: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) { this.error = 'El enlace de invitación está incompleto.'; this.loading.set(false); return; }
    this.invitations.get(this.token).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: details => this.details.set(details),
      error: err => this.error = err?.error?.message || err?.error?.mensaje || 'La invitación no existe, expiró o ya fue utilizada.'
    });
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const { password, confirmation } = this.form.getRawValue();
    if (password !== confirmation) { this.error = 'Las contraseñas no coinciden.'; return; }
    this.saving.set(true); this.error = '';
    this.invitations.accept(this.token, password!)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => this.accepted.set(true),
        error: err => this.error = err?.error?.message || err?.error?.mensaje || 'No se pudo aceptar la invitación.'
      });
  }
}
