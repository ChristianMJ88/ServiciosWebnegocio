import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';
import { TenantSiteService } from './tenant-site.service';
import { TenantSiteConfig } from './tenant.types';

describe('TenantSiteService: bootstrap de dominio propio',()=>{
 const runtime=globalThis as typeof globalThis & {__FLUORA_CUSTOM_HOST__?:string;__FLUORA_TENANT__?:TenantSiteConfig};
 const data={slug:'fixture',nombreComercial:'Fixture',publicado:true,dominioPrincipal:'marca.mx'} as TenantSiteConfig;
 afterEach(()=>{delete runtime.__FLUORA_CUSTOM_HOST__;delete runtime.__FLUORA_TENANT__;});
 it('consume una vez el HTML del backend, luego vuelve a consultar la capacidad vigente',async()=>{
  const get=vi.fn(()=>of(data));TestBed.configureTestingModule({providers:[{provide:HttpClient,useValue:{get}}]});
  runtime.__FLUORA_CUSTOM_HOST__='marca.mx';runtime.__FLUORA_TENANT__=data;const service=TestBed.inject(TenantSiteService);
  expect(await firstValueFrom(service.getByHostname('marca.mx'))).toBe(data);expect(get).not.toHaveBeenCalled();expect(runtime.__FLUORA_TENANT__).toBeUndefined();
  await firstValueFrom(service.getByHostname('marca.mx'));expect(get).toHaveBeenCalledTimes(1);
 });
 it('no reutiliza un bootstrap de otro host',async()=>{
  const get=vi.fn(()=>of(data));TestBed.configureTestingModule({providers:[{provide:HttpClient,useValue:{get}}]});runtime.__FLUORA_CUSTOM_HOST__='otro.mx';runtime.__FLUORA_TENANT__=data;
  await firstValueFrom(TestBed.inject(TenantSiteService).getByHostname('marca.mx'));expect(get).toHaveBeenCalledTimes(1);
 });
});
