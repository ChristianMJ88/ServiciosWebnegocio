import { Injectable } from '@angular/core';
import { ServicioCatalogo } from './booking-data.service';

interface ReservationDraft {
  branchId: number | null;
  serviceIds: number[];
  promocionId?: number | null;
  prestadores?: Record<number,number|null>;
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
          draft = { branchId: saved.branchId, serviceIds: [...new Set<number>(saved.serviceIds)],
            prestadores: this.normalizarPrestadores(saved.prestadores,saved.serviceIds),
            ...(Number.isSafeInteger(saved.promocionId) && saved.promocionId > 0 ? {promocionId: saved.promocionId} : {}) };
          this.drafts.set(tenantId, draft);
        }
      } catch { /* Storage may be disabled; in-memory navigation still works. */ }
    }
    return draft ? { ...draft, serviceIds: [...draft.serviceIds], prestadores: {...draft.prestadores} } : undefined;
  }

  save(tenantId: number | null, branchId: number | null, services: ServicioCatalogo[], promocionId?: number | null, prestadores?:Record<number,number|null>): void {
    if (!tenantId) return;
    const promo = promocionId === undefined ? this.read(tenantId)?.promocionId : promocionId;
    const elecciones=prestadores ?? this.read(tenantId)?.prestadores ?? {};
    const draft = { branchId, prestadores:Object.fromEntries(Object.entries(elecciones).filter(([id])=>services.some(s=>s.id===Number(id)))), serviceIds: services.map(service => service.id), ...(promo ? {promocionId: promo} : {}) };
    this.drafts.set(tenantId, draft);
    try { sessionStorage.setItem(this.key(tenantId), JSON.stringify(draft)); } catch { /* Optional persistence. */ }
  }

private normalizarPrestadores(valor:unknown,servicios:number[]):Record<number,number|null> {
  const resultado:Record<number,number|null>={};
  if(valor && typeof valor==='object' && !Array.isArray(valor)) for(const [servicio,id] of Object.entries(valor)) {
    if(servicios.includes(Number(servicio)) && (id===null || Number.isSafeInteger(id) && Number(id)>0)) resultado[Number(servicio)]=id===null?null:Number(id);
  }
  return resultado;
}
  private key(tenantId: number): string { return `fluora:reservation:${tenantId}`; }

  clear(tenantId: number | null): void {
    if (!tenantId) return;
    this.drafts.delete(tenantId);
    try { sessionStorage.removeItem(this.key(tenantId)); } catch { /* Optional persistence. */ }
  }
}
