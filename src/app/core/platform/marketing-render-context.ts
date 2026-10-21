import { InjectionToken, Provider } from '@angular/core';

export interface MarketingRenderContext {
  readonly route: '/';
  readonly marketingOrigin: string;
  readonly appOrigin: string;
}

export const MARKETING_RENDER_CONTEXT = new InjectionToken<MarketingRenderContext | null>('Marketing render context', {
  providedIn: 'root', factory: () => null
});

/** Server build configuration only. Never infer a tenant or supply invented origins. */
export function parseMarketingRenderContext(raw: unknown): MarketingRenderContext {
  const invalid = () => new Error('Invalid marketing render context');
  if (!raw || typeof raw !== 'object') throw invalid();
  const value = raw as Record<string, unknown>;
  if (value['route'] !== '/') throw invalid();
  const origin = (input: unknown): string => {
    if (typeof input !== 'string') throw invalid();
    try {
      const url = new URL(input);
      if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password
        || url.pathname !== '/' || url.search || url.hash || url.origin !== input.replace(/\/$/, '')) throw invalid();
      return url.origin;
    } catch { throw invalid(); }
  };
  const marketingOrigin = origin(value['marketingOrigin']), appOrigin = origin(value['appOrigin']);
  // Distinct ports with the same hostname would classify the build as both app and marketing.
  if (marketingOrigin !== appOrigin && new URL(marketingOrigin).hostname === new URL(appOrigin).hostname) throw invalid();
  return Object.freeze({route:'/',marketingOrigin,appOrigin});
}

export function provideMarketingRenderContext(raw: unknown): Provider {
  return {provide:MARKETING_RENDER_CONTEXT,useValue:parseMarketingRenderContext(raw)};
}

export function parseProductionMarketingRenderContext(raw: unknown): MarketingRenderContext {
  const context=parseMarketingRenderContext(raw);
  if(!context.marketingOrigin.startsWith('https://')||!context.appOrigin.startsWith('https://')||context.marketingOrigin===context.appOrigin)
    throw new Error('Invalid production marketing render origins');
  return context;
}
