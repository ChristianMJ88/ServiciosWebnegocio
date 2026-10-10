import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { MarketplaceComponent } from './marketplace.component';
import { DirectoryPage, MarketplaceService } from '../../core/marketplace/marketplace.service';
import { SeoService } from '../../core/seo/seo.service';
describe('MarketplaceComponent',()=>{
 it('cancela búsquedas anteriores y conserva el filtro enviado al paginar',()=>{
  const requests:Array<{q:string;cursor:number;response:Subject<DirectoryPage>}>=[];
  TestBed.configureTestingModule({providers:[
   {provide:MarketplaceService,useValue:{buscar:(q:string,cursor:number)=>{
    const response=new Subject<DirectoryPage>();requests.push({q,cursor,response});return response;
   }}},
   {provide:SeoService,useValue:{applyDirectory:()=>{}}}
  ]});
  const component=TestBed.runInInjectionContext(()=>new MarketplaceComponent());
  component.query='uñas';component.search();
  requests[0].response.next({negocios:[{empresaId:1,slug:'viejo',nombre:'Viejo',descripcion:null,imagenUrl:null,direccion:null,telefono:null}],siguienteCursor:null});
  expect(component.businesses()).toEqual([]);
  requests[1].response.next({negocios:[],siguienteCursor:24});
  component.query='otro filtro sin enviar';component.more();
  expect(requests[2].q).toBe('uñas');expect(requests[2].cursor).toBe(24);
 });
});
