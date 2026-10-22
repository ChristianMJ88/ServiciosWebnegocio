import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, Observable, of, throwError } from 'rxjs';
import { tenantHostResolver } from './tenant-host.resolver';
import { TenantSiteService } from './tenant-site.service';
import { TenantContextService } from './tenant-context.service';
import { PlatformHostService } from '../platform/platform-host.service';
import { TenantSiteConfig } from './tenant.types';

describe('tenantHostResolver',()=>{
 const config={slug:'fixture',nombreComercial:'Fixture',publicado:true,dominioPrincipal:'marca.mx'} as TenantSiteConfig;
 let getByHostname:ReturnType<typeof vi.fn>;
 beforeEach(()=>{
  getByHostname=vi.fn(()=>of(config));
  TestBed.configureTestingModule({providers:[provideRouter([]),
   {provide:TenantSiteService,useValue:{getByHostname}},
   {provide:TenantContextService,useValue:{setTenant:vi.fn(),clearTenant:vi.fn()}},
   {provide:PlatformHostService,useValue:{isCustomTenantHost:()=>true,currentHostname:()=> 'marca.mx',marketingUrl:(p:string)=>'https://marketing.test'+p}}
  ]});
 });
 const resolve=(url:string)=>firstValueFrom(TestBed.runInInjectionContext(()=>tenantHostResolver({} as ActivatedRouteSnapshot,{url} as RouterStateSnapshot)) as Observable<true|UrlTree>);
 for(const [url,path] of [['/servicios?utm=x','/servicios'],['/politicas','/politicas'],['/','/']])it('preserva canonical interno '+url,async()=>{
  expect(await resolve(url)).toBe(true);
  expect(document.querySelector<HTMLLinkElement>('link[rel="canonical"]')?.href).toBe('https://marca.mx'+path);
 });
 it('un fallo termina en una ruta sin resolver, sin bucle hacia la portada',async()=>{
  getByHostname.mockReturnValue(throwError(()=>new Error('No disponible')));
  const tree=await resolve('/servicios');expect(TestBed.inject(Router).serializeUrl(tree as UrlTree)).toBe('/sitio-no-disponible');expect(getByHostname).toHaveBeenCalledTimes(1);
 });
});
