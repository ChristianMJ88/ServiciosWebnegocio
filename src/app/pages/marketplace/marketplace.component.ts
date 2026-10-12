import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, catchError, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MarketplaceService, DirectoryBusiness, DirectoryFilters } from '../../core/marketplace/marketplace.service';
import { SeoService } from '../../core/seo/seo.service';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
@Component({selector:'app-marketplace',standalone:true,imports:[FormsModule,MoneyDisplayPipe],templateUrl:'./marketplace.component.html',styleUrl:'./marketplace.component.css'})
export class MarketplaceComponent {
 dayName(day:number){return ['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'][day-1] ?? '';}
 private readonly api=inject(MarketplaceService);
 private readonly requests=new Subject<{q:string;cursor:number;filters:DirectoryFilters}>();
 readonly businesses=signal<DirectoryBusiness[]>([]);
 readonly loading=signal(false);
 readonly failed=signal(false);
 readonly cursor=signal<number|null>(null);
 query='';
 ubicacion='';categoria='';
 readonly categories=signal<string[]>([]);
 readonly filtersOpen=signal(false);
 readonly activeFilters=signal<DirectoryFilters>({ubicacion:'',categoria:''});
 readonly hasFilters=computed(()=>!!this.activeFilters().ubicacion||!!this.activeFilters().categoria);
 private submittedQuery='';
 constructor(){
  inject(SeoService).applyDirectory();
  this.requests.pipe(switchMap(request=>{
   this.loading.set(true);this.failed.set(false);
   return this.api.buscar(request.q,request.cursor,request.filters).pipe(catchError(()=>of(null)));
  }),takeUntilDestroyed(inject(DestroyRef))).subscribe(page=>{
   this.loading.set(false);
   if(!page){this.failed.set(true);return;}
   this.businesses.update(previous=>[...previous,...page.negocios]);this.cursor.set(page.siguienteCursor);
   this.categories.update(previous=>Array.from(new Set([...previous,...page.negocios.flatMap(b=>(b.servicios??[]).map(s=>s.categoria).filter((c):c is string=>!!c))])).sort((a,b)=>a.localeCompare(b)));
  });
  const params=new URLSearchParams(inject(DOCUMENT).location?.search ?? '');
  this.query=params.get('q') ?? '';this.ubicacion=params.get('ubicacion') ?? '';this.categoria=params.get('categoria') ?? '';
  this.search(Number(params.get('cursor')) || 0);
 }
 search(initialCursor=0){this.filtersOpen.set(false);this.submittedQuery=this.query.trim();this.activeFilters.set({ubicacion:this.ubicacion.trim(),categoria:this.categoria.trim()});this.businesses.set([]);this.cursor.set(null);this.requests.next({q:this.submittedQuery,cursor:initialCursor,filters:this.activeFilters()});}
 clear(){this.query='';this.ubicacion='';this.categoria='';this.search();}
 more(){const cursor=this.cursor();if(cursor!==null&&!this.loading())this.requests.next({q:this.submittedQuery,cursor,filters:this.activeFilters()});}
 site(b:DirectoryBusiness){return '/e/'+encodeURIComponent(b.slug);}
}
