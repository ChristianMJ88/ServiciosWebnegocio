import { Injectable } from '@angular/core';

type RuntimeWithFluoraOrigins = typeof globalThis & {
  __FLUORA_APP_ORIGIN__?: string;
  __FLUORA_MARKETING_ORIGIN__?: string;
};

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

@Injectable({
  providedIn: 'root'
})
export class PlatformHostService {
  private readonly runtime = globalThis as RuntimeWithFluoraOrigins;
  private readonly currentOriginValue = cleanOrigin(globalThis.location?.origin);
  private readonly currentHostnameValue = globalThis.location?.hostname ?? '';
  private readonly configuredMarketingOrigin = cleanOrigin(this.runtime.__FLUORA_MARKETING_ORIGIN__);
  private readonly configuredAppOrigin = cleanOrigin(this.runtime.__FLUORA_APP_ORIGIN__);
  private readonly marketingOriginValue = this.resolveOrigin(this.configuredMarketingOrigin);
  private readonly appOriginValue = this.resolveOrigin(this.configuredAppOrigin);
  private readonly marketingHostnameValue = getHostnameFromOrigin(this.marketingOriginValue);
  private readonly appHostnameValue = getHostnameFromOrigin(this.appOriginValue);

  private resolveOrigin(configuredOrigin: string): string {
    return configuredOrigin || this.currentOriginValue;
  }

  currentHostname(): string {
    return this.currentHostnameValue;
  }

  currentOrigin(): string {
    return this.currentOriginValue;
  }

  marketingOrigin(): string {
    return this.marketingOriginValue;
  }

  appOrigin(): string {
    return this.appOriginValue;
  }

  hasDedicatedAppHost(): boolean {
    return !!this.appOriginValue
      && !!this.marketingOriginValue
      && this.appOriginValue !== this.marketingOriginValue;
  }

  isAppHost(): boolean {
    return this.hasDedicatedAppHost()
      && !!this.appHostnameValue
      && this.currentHostnameValue === this.appHostnameValue;
  }

  isMarketingHost(): boolean {
    return isMarketingHostname(this.currentHostnameValue, this.marketingHostnameValue);
  }

  isKnownPlatformHost(): boolean {
    return this.isAppHost() || this.isMarketingHost() || isLocalHost(this.currentHostnameValue);
  }

  isCustomTenantHost(): boolean {
    return !this.isKnownPlatformHost();
  }

  appUrl(path = ''): string {
    return `${this.appOriginValue}${path.startsWith('/') ? path : `/${path}`}`;
  }

  marketingUrl(path = ''): string {
    return `${this.marketingOriginValue}${path.startsWith('/') ? path : `/${path}`}`;
  }
}
