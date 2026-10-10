import { Injectable, signal } from '@angular/core';

export const CAMPAIGN_KEYS = ['utm_source', 'utm_medium', 'utm_campaign', 'utm_content', 'utm_term'] as const;
type CampaignKey = typeof CAMPAIGN_KEYS[number];
export interface MarketingAttribution {
  vertical?: string;
  campaign: Partial<Record<CampaignKey, string>>;
}
export type MarketingEvent = 'landing_view' | 'signup_click' | 'signup_started' | 'signup_completed' | 'business_created';

// Solo identificadores de campaña: nunca copiar query strings completas ni referrers.
export function campaignIdentifier(value: unknown): string | undefined {
  if (typeof value !== 'string' || !/^[a-zA-Z][a-zA-Z0-9_-]{0,79}$/.test(value) || /\d{10,}/.test(value)) return undefined;
  return value;
}
export function readAttribution(params: URLSearchParams, vertical?: string): MarketingAttribution {
  const campaign: MarketingAttribution['campaign'] = {};
  for (const key of CAMPAIGN_KEYS) {
    const value = campaignIdentifier(params.get(key));
    if (value) campaign[key] = value;
  }
  return { campaign, vertical: campaignIdentifier(vertical || params.get('landing_vertical')) };
}

type TrackingRuntime = typeof globalThis & {
  __FLUORA_MARKETING_TRACKING__?: { enabled?: boolean };
  dataLayer?: Record<string, unknown>[];
};
@Injectable({ providedIn: 'root' })
export class MarketingTrackingService {
  private readonly storageKey = 'fluora_marketing_attribution';
  private readonly context = signal<MarketingAttribution>(this.restore());
  private consent = false;

  capture(search: string, vertical?: string): void {
    const incoming = readAttribution(new URLSearchParams(search), vertical);
    if (!incoming.vertical && !Object.keys(incoming.campaign).length) return;
    this.context.set({
      vertical: incoming.vertical || this.context().vertical,
      campaign: Object.keys(incoming.campaign).length ? incoming.campaign : this.context().campaign
    });
    try { globalThis.sessionStorage?.setItem(this.storageKey, JSON.stringify(this.context())); } catch { /* El registro no depende del almacenamiento. */ }
  }

  registrationUrl(base: string, planId?: number): string {
    const url = new URL(base, globalThis.location?.origin);
    const context = this.context();
    if (planId !== undefined) url.searchParams.set('plan', String(planId));
    for (const key of CAMPAIGN_KEYS) {
      const value = context.campaign[key];
      if (value) url.searchParams.set(key, value);
    }
    if (context.vertical) url.searchParams.set('landing_vertical', context.vertical);
    return url.toString();
  }

  // Conectar con el consentimiento explícito del visitante antes de habilitar GTM.
  setConsent(granted: boolean): void { this.consent = granted; }

  emit(event: MarketingEvent): void {
    const runtime = globalThis as TrackingRuntime;
    if (!this.consent || runtime.__FLUORA_MARKETING_TRACKING__?.enabled !== true) return;
    // No enviar términos, contenido libre, URL, empresa, formulario ni identificadores de clientes.
    const payload: Record<string, unknown> = { event };
    if (this.context().vertical) payload['vertical'] = this.context().vertical;
    for (const [utm, output] of [['utm_source', 'source'], ['utm_medium', 'medium'], ['utm_campaign', 'campaign']] as const) {
      const value = this.context().campaign[utm];
      if (value) payload[output] = value;
    }
    try { (runtime.dataLayer ??= []).push(payload); } catch { /* Un proveedor de medición no puede bloquear el registro. */ }
  }

  private restore(): MarketingAttribution {
    try {
      const stored = JSON.parse(globalThis.sessionStorage?.getItem(this.storageKey) || 'null');
      const params = new URLSearchParams();
      for (const key of CAMPAIGN_KEYS) if (typeof stored?.campaign?.[key] === 'string') params.set(key, stored.campaign[key]);
      return readAttribution(params, stored?.vertical);
    } catch { return { campaign: {} }; }
  }
}
