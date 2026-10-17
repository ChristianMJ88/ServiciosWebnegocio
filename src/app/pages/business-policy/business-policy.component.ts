import { Component, inject, OnInit, signal, DestroyRef } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { BusinessPolicy, BusinessPolicyService } from '../../core/policies/business-policy.service';
@Component({selector: 'app-business-policy', standalone: true, imports: [DatePipe, RouterLink],
  templateUrl: './business-policy.component.html', styleUrl: './business-policy.component.css'})
export class BusinessPolicyComponent implements OnInit {
  readonly tenant = inject(TenantContextService);
  private readonly api = inject(BusinessPolicyService);
  private readonly destroyRef = inject(DestroyRef);
  readonly policy = signal<BusinessPolicy | null>(null);
  readonly loading = signal(true);
  ngOnInit() {
    const slug = this.tenant.slug();
    if (!slug) { this.loading.set(false); return; }
    this.api.publicPolicy(slug).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: p => {this.policy.set(p); this.loading.set(false);}, error: () => this.loading.set(false)
    });
  }
}
