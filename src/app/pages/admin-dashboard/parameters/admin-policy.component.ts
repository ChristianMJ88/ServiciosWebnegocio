import { Component, inject, OnInit, signal, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BusinessPolicyService, AdminPolicy, PolicyDraft } from '../../../core/policies/business-policy.service';
@Component({selector: 'app-admin-policy', standalone: true, imports: [FormsModule],
  templateUrl: './admin-policy.component.html', styleUrl: './admin-policy.component.css'})
export class AdminPolicyComponent implements OnInit {
  private readonly api = inject(BusinessPolicyService);
  private readonly destroyRef = inject(DestroyRef);
  readonly data = signal<AdminPolicy | null>(null); readonly error = signal(''); readonly success = signal('');
  readonly busy = signal(false); readonly preview = signal(''); draft: PolicyDraft | null = null;
  ngOnInit() { this.load(); }
  load() { this.api.admin().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next: d => this.apply(d), error: e => this.fail(e)}); }
  changed() { this.preview.set(''); this.success.set(''); }
  showPreview() {
    if (!this.draft) return; this.busy.set(true); this.error.set('');
    this.api.preview(this.draft).subscribe({next: p => {this.preview.set(p.texto); this.busy.set(false);}, error: e => this.fail(e)});
  }
  publish() {
    if (!this.draft || !this.preview() || this.busy()) return;
    this.busy.set(true); this.error.set('');
    this.api.publish(this.draft).subscribe({next: d => {this.apply(d); this.success.set('Política publicada. Las reservas anteriores conservan su versión.'); this.busy.set(false);}, error: e => this.fail(e)});
  }
  private apply(d: AdminPolicy) { this.data.set(d); this.draft = {...d.borrador}; this.preview.set(''); }
  private fail(e: any) { this.error.set(e?.error?.mensaje || e?.error?.message || e?.error?.detail || 'No se pudo consultar o publicar la política.'); this.busy.set(false); }
}
