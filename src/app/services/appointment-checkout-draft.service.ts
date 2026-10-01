import { Injectable } from '@angular/core';

export interface AppointmentCheckoutDraft {
  appointmentIds: number[];
  branchId: number;
  name: string;
  email: string;
  phone: string;
}

@Injectable({ providedIn: 'root' })
export class AppointmentCheckoutDraftService {
  save(tenantId: number, draft: AppointmentCheckoutDraft): void {
    sessionStorage.setItem(this.key(tenantId), JSON.stringify(draft));
  }

  read(tenantId: number): AppointmentCheckoutDraft | null {
    try {
      const raw = sessionStorage.getItem(this.key(tenantId));
      return raw ? JSON.parse(raw) as AppointmentCheckoutDraft : null;
    } catch {
      return null;
    }
  }

  clear(tenantId: number): void {
    sessionStorage.removeItem(this.key(tenantId));
  }

  private key(tenantId: number): string {
    return `fluora:checkout:citas:${tenantId}`;
  }
}
