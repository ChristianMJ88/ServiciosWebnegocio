import { Injectable, computed, inject, signal } from '@angular/core';
import { PlanPublicoFluora, PublicPlansService } from '../platform/public-plans.service';
import { PlatformHostService } from '../platform/platform-host.service';
import { MarketingTrackingService } from './marketing-tracking.service';
/** One presentation of the public offer; prices, limits and trial periods always come from the API. */
@Injectable({providedIn:'root'})
export class MarketingOfferService {
  private readonly api=inject(PublicPlansService);
  private readonly host=inject(PlatformHostService);
  private readonly tracking=inject(MarketingTrackingService);
  private started=false;
  readonly plans=signal<PlanPublicoFluora[]>([]);
  readonly loading=signal(true);
  readonly failed=signal(false);
  readonly fallbackLabel='Crear mi cuenta';
  readonly recommended=computed(()=>this.plans().find(p=>p.destacado) ?? this.plans()[0] ?? null);
  readonly href=computed(()=>this.tracking.registrationUrl(this.host.appUrl('/registro'),this.recommended()?.id));
  readonly label=computed(()=>{const plan=this.recommended();return plan?this.planLabel(plan):this.fallbackLabel;});
  readonly shortLabel=computed(()=>this.recommended()?.diasPrueba ? 'Probar' : 'Empezar');
  load(){if(this.started)return;this.started=true;this.loading.set(true);this.failed.set(false);this.api.listar().subscribe({next:p=>{this.plans.set(p);this.loading.set(false);},error:()=>{this.started=false;this.failed.set(true);this.loading.set(false);}});}
  planHref(id:number){return this.tracking.registrationUrl(this.host.appUrl('/registro'),id);}
  planLabel(plan:PlanPublicoFluora){return plan.diasPrueba>0?`Probar ${plan.diasPrueba} días`:`Comenzar con ${plan.nombre}`;}
  signup(){this.tracking.emit('signup_click');}
}
