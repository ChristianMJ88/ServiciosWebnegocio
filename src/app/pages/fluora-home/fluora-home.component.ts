import { MarketingSectionsDirective } from '../../core/marketing/marketing-sections.directive';
import { DOCUMENT } from '@angular/common';
import { afterEveryRender, AfterViewInit, ChangeDetectionStrategy, computed, Component, DestroyRef, ElementRef, NgZone, OnDestroy, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import type { gsap } from 'gsap';
import { MarketingAppearanceService } from '../../core/marketing/marketing-appearance.service';
import { MarketingOfferService } from '../../core/marketing/marketing-offer.service';
import { LandingMarketingService } from '../../core/marketing/landing-marketing.service';
import { PlatformContactService, PublicSupport } from '../../core/marketing/platform-contact.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';
import { SeoService } from '../../core/seo/seo.service';
import { HomeHeroComponent } from './sections/hero.component';
import { HomePillarsComponent } from './sections/pillars.component';
import { HomeWorkflowComponent } from './sections/workflow.component';
import { HomeProductComponent } from './sections/product.component';
import { HomePlansComponent } from './sections/plans.component';
import { HomeFaqComponent } from './sections/faq.component';
import { HomeClosingComponent } from './sections/closing.component';
@Component({selector:'app-fluora-home',standalone:true,imports:[MarketingSectionsDirective,HomeHeroComponent,HomePillarsComponent,HomeWorkflowComponent,HomeProductComponent,HomePlansComponent,HomeFaqComponent,HomeClosingComponent],templateUrl:'./fluora-home.component.html',styleUrl:'./fluora-home.component.css',changeDetection:ChangeDetectionStrategy.OnPush})
export class FluoraHomeComponent implements AfterViewInit,OnDestroy {
  readonly platformHost=inject(PlatformHostService);
  readonly offer=inject(MarketingOfferService);
  readonly recommendedRegistrationHref=this.offer.href;
  readonly recommendedCta=this.offer.label;
  readonly support=signal<PublicSupport|null>(null);
  readonly verticals=signal<{slug:string;titulo:string;descripcion:string}[]>([]);
  readonly renderAnchors=signal(false);
  private readonly element=inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly document=inject(DOCUMENT);
  private readonly zone=inject(NgZone);
  private readonly product=viewChild('productReady',{read:ElementRef});
  private readonly plansSection=viewChild('plansReady',{read:ElementRef});
  private readonly faq=viewChild('faqReady',{read:ElementRef});
  private readonly closing=viewChild('closingReady',{read:ElementRef});
  private pendingAnchor:string|null=null;
  private contactsReady=signal(false);
  private verticalsReady=signal(false);
  readonly dataReady=computed(()=>this.contactsReady()&&this.verticalsReady()&&!this.offer.loading());
  private destroyed=false;
  private animation?:gsap.Context;
  private releaseAppearance=()=>{};
  constructor(){
    const router=inject(Router),seo=inject(SeoService),meta=inject(Meta),title=inject(Title);
    if(!this.platformHost.isCustomTenantHost()){
      this.releaseAppearance=inject(MarketingAppearanceService).activate();
      inject(PlatformContactService).publicSupport().pipe(takeUntilDestroyed()).subscribe(value=>{this.support.set(value);this.contactsReady.set(true);});
    }else this.contactsReady.set(true);
    inject(LandingMarketingService).navegacion().pipe(takeUntilDestroyed()).subscribe({next:links=>{this.verticals.set(links);this.verticalsReady.set(true);},error:()=>{this.verticalsReady.set(true);}});
    this.offer.load();
    seo.applyMarketing();
    title.setTitle('Fluora | Agenda en línea para salones, barberías y spas');
    const description='Organiza citas, equipo y clientes con Fluora. Publica tu propio sitio con reservas y tienda en línea. Consulta los módulos y precios de cada plan.';
    meta.updateTag({name:'description',content:description});
    meta.updateTag({name:'keywords',content:'agenda en línea, salones, barberías, spas, reservas, tienda en línea, Fluora'});
    meta.updateTag({property:'og:title',content:'Fluora | Tu negocio, conectado'});
    meta.updateTag({property:'og:description',content:description});
    meta.updateTag({property:'og:type',content:'website'});
    if(this.platformHost.hasDedicatedAppHost()&&this.platformHost.isAppHost())void router.navigateByUrl('/acceso');
    inject(ActivatedRoute).fragment.pipe(takeUntilDestroyed()).subscribe(fragment=>{this.pendingAnchor=fragment;this.renderAnchors.set(!!fragment);});
    // Anchors wait for deferred sections and preceding API content, not a timeout.
    afterEveryRender(()=>{
      if(!this.pendingAnchor||!this.product()||!this.plansSection()||!this.faq()||!this.closing()||!this.dataReady())return;
      const target=[...this.element.nativeElement.querySelectorAll<HTMLElement>('[id]')].find(e=>e.id===this.pendingAnchor);
      this.pendingAnchor=null;
      target?.scrollIntoView({block:'start',behavior:'auto'});
    });
    const cancelAnchor=()=>{this.pendingAnchor=null;};
    const cancelKey=(event:KeyboardEvent)=>{if(['PageDown','PageUp','Home','End','ArrowDown','ArrowUp',' '].includes(event.key))cancelAnchor();};
    const win=this.document.defaultView;
    win?.addEventListener('wheel',cancelAnchor,{passive:true});win?.addEventListener('touchstart',cancelAnchor,{passive:true});win?.addEventListener('keydown',cancelKey);
    inject(DestroyRef).onDestroy(()=>{win?.removeEventListener('wheel',cancelAnchor);win?.removeEventListener('touchstart',cancelAnchor);win?.removeEventListener('keydown',cancelKey);});
  }
  registroPlanHref(id:number){return this.offer.planHref(id);}
  async ngAfterViewInit(){
    const win=this.document.defaultView;
    if(!win||win.matchMedia('(prefers-reduced-motion:reduce)').matches||!win.matchMedia('(min-width:1200px)').matches)return;
    const {gsap}=await this.loadAnimation();
    if(this.destroyed||win.matchMedia('(prefers-reduced-motion:reduce)').matches||!win.matchMedia('(min-width:1200px)').matches)return;
    this.zone.runOutsideAngular(()=>{this.animation=gsap.context(()=>{
      // One brief decorative reveal. Heading and CTA are never hidden.
      gsap.to('.fl-home-preview-icon',{scale:1.06,duration:.3,repeat:1,yoyo:true,ease:'power2.out',clearProps:'transform'});
    },this.element.nativeElement);});
  }
  private loadAnimation(){return import('gsap');}
  ngOnDestroy(){this.destroyed=true;this.animation?.revert();this.releaseAppearance();}
}
