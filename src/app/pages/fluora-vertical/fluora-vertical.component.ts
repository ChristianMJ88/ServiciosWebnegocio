import { Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LandingMarketingService, MarketingLanding } from '../../core/marketing/landing-marketing.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';
import { SeoService } from '../../core/seo/seo.service';
import { PublicPlansService, PlanPublicoFluora } from '../../core/platform/public-plans.service';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
@Component({
 selector:'app-fluora-vertical',standalone:true,imports:[MoneyDisplayPipe],
 templateUrl:'./fluora-vertical.component.html',styleUrl:'./fluora-vertical.component.css'
})
export class FluoraVerticalComponent {
 readonly host=inject(PlatformHostService);
 private readonly element=inject<ElementRef<HTMLElement>>(ElementRef);
 private readonly seo=inject(SeoService);
 readonly showSticky=signal(false);
 private actionObserver?:IntersectionObserver;
 @ViewChild('primaryAction') set primaryAction(ref:ElementRef<HTMLAnchorElement>|undefined){
  this.actionObserver?.disconnect();
  if(!ref||typeof IntersectionObserver==='undefined')return;
  this.actionObserver=new IntersectionObserver(([entry])=>this.showSticky.set(!entry.isIntersecting),{threshold:0});
  this.actionObserver.observe(ref.nativeElement);
 }
 readonly landing=signal<MarketingLanding|null>(null);
 readonly plans=signal<PlanPublicoFluora[]>([]);
 readonly failed=signal(false);
 readonly loading=signal(true);
 readonly plansFailed=signal(false);
 readonly registrationHref=this.host.appUrl('/registro');
 constructor(){
  inject(DestroyRef).onDestroy(()=>this.actionObserver?.disconnect());
  const slug=inject(ActivatedRoute).snapshot.data['vertical'] as string;
  inject(LandingMarketingService).publica(slug).pipe(takeUntilDestroyed()).subscribe({
   next:l=>{this.landing.set(l);this.loading.set(false);this.seo.applyLanding(l);},
   error:()=>{this.loading.set(false);this.failed.set(true);this.seo.applyUnavailableMarketing();}
  });
  inject(PublicPlansService).listar().pipe(takeUntilDestroyed()).subscribe({next:p=>this.plans.set(p),error:()=>this.plansFailed.set(true)});
 }
 verPasos(){this.element.nativeElement.querySelector<HTMLElement>('#como-funciona')?.scrollIntoView({behavior:'auto',block:'start'});}
 registroPlan(p:PlanPublicoFluora){return this.registrationHref+'?plan='+p.id;}
}
