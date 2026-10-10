import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, catchError, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MarketplaceService, DirectoryBusiness } from '../../core/marketplace/marketplace.service';
import { SeoService } from '../../core/seo/seo.service';
@Component({selector:'app-marketplace',standalone:true,imports:[FormsModule],templateUrl:'./marketplace.component.html',styleUrl:'./marketplace.component.css'})
export class MarketplaceComponent {
 private readonly api=inject(MarketplaceService);
 private readonly requests=new Subject<{q:string;cursor:number}>();
 readonly businesses=signal<DirectoryBusiness[]>([]);
 readonly loading=signal(false);
 readonly failed=signal(false);
 readonly cursor=signal<number|null>(null);
 query='';
 private submittedQuery='';
 constructor(){
  inject(SeoService).applyDirectory();
  this.requests.pipe(switchMap(request=>{
   this.loading.set(true);this.failed.set(false);
   return this.api.buscar(request.q,request.cursor).pipe(catchError(()=>of(null)));
  }),takeUntilDestroyed(inject(DestroyRef))).subscribe(page=>{
   this.loading.set(false);
   if(!page){this.failed.set(true);return;}
   this.businesses.update(previous=>[...previous,...page.negocios]);this.cursor.set(page.siguienteCursor);
  });
  this.search();
 }
 search(){this.submittedQuery=this.query.trim();this.businesses.set([]);this.cursor.set(null);this.requests.next({q:this.submittedQuery,cursor:0});}
 more(){const cursor=this.cursor();if(cursor!==null&&!this.loading())this.requests.next({q:this.submittedQuery,cursor});}
 site(b:DirectoryBusiness){return '/e/'+encodeURIComponent(b.slug);}
}
