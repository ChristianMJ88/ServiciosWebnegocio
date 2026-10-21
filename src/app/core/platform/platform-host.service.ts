import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { MARKETING_RENDER_CONTEXT, MarketingRenderContext } from './marketing-render-context';

export type RuntimeWithFluoraOrigins = {
  __FLUORA_APP_ORIGIN__?: string;
  __FLUORA_MARKETING_ORIGIN__?: string;
};

export type HostInput =
  | {kind:'browser';location:Pick<Location,'origin'|'hostname'>|undefined;runtime:RuntimeWithFluoraOrigins}
  | {kind:'server';context:MarketingRenderContext|null};
export interface HostSnapshot {
  readonly currentOrigin:string;
  readonly currentHostname:string;
  readonly marketingOrigin:string;
  readonly appOrigin:string;
}

export function resolvePlatformHostSnapshot(input: HostInput): HostSnapshot {
  if (input.kind === 'server') {
    if (!input.context) throw new Error('Server render requires an explicit marketing context');
    return {currentOrigin:input.context.marketingOrigin,currentHostname:getHostnameFromOrigin(input.context.marketingOrigin),
      marketingOrigin:input.context.marketingOrigin,appOrigin:input.context.appOrigin};
  }
  const currentOrigin=cleanOrigin(input.location?.origin);
  return {currentOrigin,currentHostname:input.location?.hostname??'',
    marketingOrigin:cleanOrigin(input.runtime.__FLUORA_MARKETING_ORIGIN__)||currentOrigin,
    appOrigin:cleanOrigin(input.runtime.__FLUORA_APP_ORIGIN__)||currentOrigin};
}

function cleanOrigin(origin: string | null | undefined): string {
  return (origin ?? '').trim().replace(/\/+$/, '');
}

function getHostnameFromOrigin(origin: string): string {
  if (!origin) {
    return '';
  }

  try {
    return new URL(origin).hostname;
  } catch {
    return '';
  }
}

function isLocalHost(hostname: string): boolean {
  return hostname === 'localhost'
    || hostname === '127.0.0.1'
    || hostname === '::1'
    || /^192\.168\.\d{1,3}\.\d{1,3}$/.test(hostname)
    || /^10\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(hostname)
    || /^172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3}$/.test(hostname);
}

export function isMarketingHostname(currentHostname: string, marketingHostname: string): boolean {
  if (!currentHostname || !marketingHostname) {
    return false;
  }

  return currentHostname === marketingHostname
    || currentHostname === `www.${marketingHostname}`;
}

/** Only the marketing root can reuse server-rendered Angular DOM; other routes stay CSR. */
export function isHydratableMarketingHome(location: Pick<Location, 'origin'|'hostname'|'pathname'>|undefined,
    runtime: RuntimeWithFluoraOrigins): boolean {
  if (!location || location.pathname !== '/') return false;
  const configuredOrigin=runtime.__FLUORA_MARKETING_ORIGIN__;
  if(typeof configuredOrigin!=='string'||!configuredOrigin.trim())return false;
  const snapshot = resolvePlatformHostSnapshot({kind:'browser',location,runtime});
  return isMarketingHostname(snapshot.currentHostname, getHostnameFromOrigin(snapshot.marketingOrigin));
}

@Injectable({
  providedIn: 'root'
})
export class PlatformHostService {
  private readonly snapshot = resolvePlatformHostSnapshot(isPlatformBrowser(inject(PLATFORM_ID))
    ? {kind:'browser',location:globalThis.location,runtime:globalThis as RuntimeWithFluoraOrigins}
    : {kind:'server',context:inject(MARKETING_RENDER_CONTEXT)});
  private readonly marketingHost = getHostnameFromOrigin(this.snapshot.marketingOrigin);
  private readonly appHost = getHostnameFromOrigin(this.snapshot.appOrigin);

  currentHostname(): string {
    return this.snapshot.currentHostname;
  }

  currentOrigin(): string {
    return this.snapshot.currentOrigin;
  }

  marketingOrigin(): string {
    return this.snapshot.marketingOrigin;
  }

  appOrigin(): string {
    return this.snapshot.appOrigin;
  }

  hasDedicatedAppHost(): boolean {
    return !!this.snapshot.appOrigin
      && !!this.snapshot.marketingOrigin
      && this.snapshot.appOrigin !== this.snapshot.marketingOrigin;
  }

  isAppHost(): boolean {
    return this.hasDedicatedAppHost()
      && !!this.appHost
      && this.snapshot.currentHostname === this.appHost;
  }

  isMarketingHost(): boolean {
    return isMarketingHostname(this.snapshot.currentHostname, this.marketingHost);
  }

  isKnownPlatformHost(): boolean {
    return this.isAppHost() || this.isMarketingHost() || isLocalHost(this.snapshot.currentHostname);
  }

  isCustomTenantHost(): boolean {
    return !this.isKnownPlatformHost();
  }

  appUrl(path = ''): string {
    return `${this.snapshot.appOrigin}${path.startsWith('/') ? path : `/${path}`}`;
  }

  marketingUrl(path = ''): string {
    return `${this.snapshot.marketingOrigin}${path.startsWith('/') ? path : `/${path}`}`;
  }
}
