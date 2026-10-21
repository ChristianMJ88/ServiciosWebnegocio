import { TestBed } from '@angular/core/testing';
import { PLATFORM_ID, EnvironmentInjector, createEnvironmentInjector } from '@angular/core';
import { afterEach, vi } from 'vitest';
import { MARKETING_RENDER_CONTEXT, parseMarketingRenderContext, parseProductionMarketingRenderContext, provideMarketingRenderContext } from './marketing-render-context';
import { PlatformHostService, resolvePlatformHostSnapshot } from './platform-host.service';

const config = {route:'/',marketingOrigin:'https://marketing.test',appOrigin:'https://app.test'};
describe('contexto y host: contrato de servidor simulado', () => {
  afterEach(() => vi.unstubAllGlobals());
  it('el contexto productivo exige HTTPS y orígenes separados, sin defaults',()=>{
    expect(parseProductionMarketingRenderContext(config)).toEqual(config);
    for(const raw of [undefined,{...config,route:'/e/otro'},{...config,marketingOrigin:'http://marketing.test'},{...config,appOrigin:config.marketingOrigin}])expect(()=>parseProductionMarketingRenderContext(raw)).toThrow();
  });
  it.each(['https','http'])('valida y congela orígenes %s sin valores de respaldo', protocol => {
    const value = parseMarketingRenderContext({route:'/',marketingOrigin:`${protocol}://marketing.test/`,appOrigin:`${protocol}://app.test`});
    expect(value.marketingOrigin).toBe(`${protocol}://marketing.test`);
    expect(Object.isFrozen(value)).toBe(true);
  });
  it.each([null,{}, {route:'/e/tenant'}, {route:'/',marketingOrigin:12,appOrigin:'https://app.test'},
    ...['https://user:private-value@marketing.test','https://marketing.test/path','https://marketing.test/?secret=private-value',
      'https://marketing.test/#private-value','ftp://marketing.test','javascript:private-value','not-an-origin']
      .map(marketingOrigin=>({...config,marketingOrigin})),
    {...config,appOrigin:'https://user:private-value@app.test'},
    {...config,appOrigin:'https://marketing.test:444'}
  ])('rechaza configuración inválida sin reflejar su contenido (%j)', raw => {
    expect(()=>parseMarketingRenderContext(raw)).toThrow('Invalid marketing render context');
    try{parseMarketingRenderContext(raw);}catch(error){expect((error as Error).message).not.toContain('private-value');}
  });
  it('permite el mismo origen integrado sin convertirlo en host de aplicación separado', () => {
    TestBed.configureTestingModule({providers:[{provide:PLATFORM_ID,useValue:'server'},provideMarketingRenderContext({...config,appOrigin:config.marketingOrigin})]});
    const host=TestBed.inject(PlatformHostService);
    expect(host.isAppHost()).toBe(false);expect(host.isMarketingHost()).toBe(true);expect(host.isCustomTenantHost()).toBe(false);
  });
  it('usa únicamente el contexto de servidor y no consulta ubicación ni runtime tóxicos', () => {
    const context = parseMarketingRenderContext(config);
    const input = {kind:'server' as const,context,get location(){throw Error('location read');},get runtime(){throw Error('runtime read');}};
    expect(resolvePlatformHostSnapshot(input)).toEqual({currentOrigin:config.marketingOrigin,currentHostname:'marketing.test',marketingOrigin:config.marketingOrigin,appOrigin:config.appOrigin});
    vi.stubGlobal('location',new Proxy({},{get(){throw Error('Browser location read');}}));
    vi.stubGlobal('__FLUORA_MARKETING_ORIGIN__',{trim(){throw Error('Browser runtime read');}});
    TestBed.configureTestingModule({providers:[{provide:PLATFORM_ID,useValue:'server'},provideMarketingRenderContext(config)]});
    const host = TestBed.inject(PlatformHostService);
    expect(host.isMarketingHost()).toBe(true);
    expect(host.isAppHost()).toBe(false);
    expect(host.isCustomTenantHost()).toBe(false);
    expect(host.appUrl('/registro')).toBe('https://app.test/registro');
  });
  it('falla cerrado cuando no se declara identidad para el servidor', () => {
    TestBed.configureTestingModule({providers:[{provide:PLATFORM_ID,useValue:'server'}]});
    expect(()=>TestBed.inject(PlatformHostService)).toThrow('Server render requires an explicit marketing context');
  });
  it('el navegador ignora el contexto de servidor y conserva su host real', () => {
    vi.stubGlobal('location',{origin:'https://reservas.negocio.test',hostname:'reservas.negocio.test'});
    vi.stubGlobal('__FLUORA_MARKETING_ORIGIN__','https://client-marketing.test');
    vi.stubGlobal('__FLUORA_APP_ORIGIN__','https://client-app.test');
    TestBed.configureTestingModule({providers:[provideMarketingRenderContext(config)]});
    const host = TestBed.inject(PlatformHostService);
    expect(host.currentHostname()).toBe('reservas.negocio.test');
    expect(host.isCustomTenantHost()).toBe(true);
    expect(host.appUrl('/registro')).toBe('https://client-app.test/registro');
  });
  it('mantiene contextos independientes por injector, sin estado global del proceso', () => {
    const parent=TestBed.inject(EnvironmentInjector);
    const first=createEnvironmentInjector([PlatformHostService,{provide:PLATFORM_ID,useValue:'server'},provideMarketingRenderContext(config)],parent);
    const second=createEnvironmentInjector([PlatformHostService,{provide:PLATFORM_ID,useValue:'server'},provideMarketingRenderContext({...config,marketingOrigin:'https://second.test',appOrigin:'https://second-app.test'})],parent);
    try {
      expect(first.get(PlatformHostService).appUrl('/registro')).toBe('https://app.test/registro');
      expect(second.get(PlatformHostService).appUrl('/registro')).toBe('https://second-app.test/registro');
      expect(first.get(MARKETING_RENDER_CONTEXT)).not.toBe(second.get(MARKETING_RENDER_CONTEXT));
    } finally {first.destroy();second.destroy();}
  });
});
