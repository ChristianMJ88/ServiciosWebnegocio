import { Injectable } from '@angular/core';
import { ServicioCatalogo } from './booking-data.service';

interface ReservationDraft {
  branchId: number | null;
  serviceIds: number[];
}

/** Only IDs are retained. Prices and availability always come from the current catalog. */
@Injectable({ providedIn: 'root' })
export class ReservationCartService {
  private readonly drafts = new Map<number, ReservationDraft>();

  read(tenantId: number | null): ReservationDraft | undefined {
    if (!tenantId) return undefined;
    let draft = this.drafts.get(tenantId);
    if (!draft) {
      try {
        const saved = JSON.parse(sessionStorage.getItem(this.key(tenantId)) || 'null');
        if (saved && (saved.branchId === null || Number.isSafeInteger(saved.branchId) && saved.branchId > 0) &&
            Array.isArray(saved.serviceIds) && saved.serviceIds.every((id: unknown) => Number.isSafeInteger(id) && Number(id) > 0)) {
          draft = { branchId: saved.branchId, serviceIds: [...new Set<number>(saved.serviceIds)] };
          this.drafts.set(tenantId, draft);
        }
      } catch { /* Storage may be disabled; in-memory navigation still works. */ }
    }
    return draft ? { ...draft, serviceIds: [...draft.serviceIds] } : undefined;
  }

  save(tenantId: number | null, branchId: number | null, services: ServicioCatalogo[]): void {
    if (!tenantId) return;
    const draft = { branchId, serviceIds: services.map(service => service.id) };
    this.drafts.set(tenantId, draft);
    try { sessionStorage.setItem(this.key(tenantId), JSON.stringify(draft)); } catch { /* Optional persistence. */ }
  }

  private key(tenantId: number): string { return `fluora:reservation:${tenantId}`; }

  clear(tenantId: number | null): void {
    if (!tenantId) return;
    this.drafts.delete(tenantId);
    try { sessionStorage.removeItem(this.key(tenantId)); } catch { /* Optional persistence. */ }
  }
}
