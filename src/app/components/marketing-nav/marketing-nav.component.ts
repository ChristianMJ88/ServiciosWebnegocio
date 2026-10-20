import { MarketingSectionsService } from '../../core/marketing/marketing-sections.service';
import { hasMarketingTabs, marketingPath } from '../../core/marketing/marketing-route';
import { DOCUMENT, Location, isPlatformBrowser } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, NgZone, PLATFORM_ID, ViewChild, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { LandingMarketingService } from '../../core/marketing/landing-marketing.service';
import { MarketingOfferService } from '../../core/marketing/marketing-offer.service';
import { MarketingAppearanceService } from '../../core/marketing/marketing-appearance.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

export function marketingDetached(previous:boolean,y:number):boolean{return y>64?true:y<16?false:previous;}
@Component({selector:'app-marketing-nav',standalone:true,templateUrl:'./marketing-nav.component.html',styleUrl:'./marketing-nav.component.css',changeDetection:ChangeDetectionStrategy.OnPush,host:{class:'fluora-marketing'}})
export class MarketingNavComponent {
  private readonly router=inject(Router);
  private readonly location=inject(Location);
  private readonly document=inject(DOCUMENT);
  private readonly zone=inject(NgZone);
  readonly host=inject(PlatformHostService);
  readonly offer=inject(MarketingOfferService);
  readonly links=signal<{slug:string;titulo:string;descripcion:string}[]>([]);
  readonly detached=signal(false);
  readonly menuOpen=signal(false);
  readonly active=inject(MarketingSectionsService).active;
  private readonly navigation=toSignal(this.router.events.pipe(filter((e):e is NavigationEnd=>e instanceof NavigationEnd)));
  private readonly route=computed(()=>this.navigation()?.urlAfterRedirects ?? (this.router.navigated?this.router.url:this.location.path()));
  readonly isHome=computed(()=>marketingPath(this.route()||'/')==='/');
  readonly mobileTabs=computed(()=>{this.navigation();return hasMarketingTabs(this.router.routerState.snapshot.root,this.route()||'/');});
  readonly sections=[{id:'producto',label:'Producto',icon:'bi-grid'},{id:'como-funciona',label:'Cómo funciona',icon:'bi-play-circle'},{id:'planes',label:'Planes',icon:'bi-layers'},{id:'faq',label:'Preguntas',icon:'bi-question-circle'}];
  @ViewChild('menu',{static:true}) private menu!:ElementRef<HTMLDialogElement>;
  @ViewChild('business',{static:true}) private business!:ElementRef<HTMLDetailsElement>;
  private opener:HTMLElement|null=null;
  private oldOverflow='';private locked=false;
  constructor(){
    this.offer.load();
    inject(LandingMarketingService).navegacion().pipe(takeUntilDestroyed()).subscribe({next:links=>this.links.set(links),error:()=>this.links.set([])});
    const releaseAppearance=inject(MarketingAppearanceService).activate(true);
    inject(DestroyRef).onDestroy(releaseAppearance);
    if(!isPlatformBrowser(inject(PLATFORM_ID)))return;
    const root=this.document.documentElement,oldGutter=root.style.scrollbarGutter;
    root.style.scrollbarGutter='stable';
    this.router.events.pipe(filter(e=>e instanceof NavigationEnd),takeUntilDestroyed()).subscribe(()=>this.closeMenu());
    const win=this.document.defaultView;
    const onScroll=()=>{const next=marketingDetached(this.detached(),win?.scrollY||0);if(next!==this.detached())this.zone.run(()=>this.detached.set(next));};
    const onClick=(event:Event)=>{if(!this.business?.nativeElement.contains(event.target as Node))this.business?.nativeElement.removeAttribute('open');};
    const onKey=(event:KeyboardEvent)=>{if(event.key==='Escape' && this.business?.nativeElement.open && !this.menuOpen()){this.business.nativeElement.removeAttribute('open');this.business.nativeElement.querySelector('summary')?.focus();event.preventDefault();}};
    const desktop=win?.matchMedia?.('(min-width:1200px)');
    const onDesktop=()=>{if(desktop?.matches)this.zone.run(()=>this.closeMenu());};
    this.zone.runOutsideAngular(()=>{win?.addEventListener('scroll',onScroll,{passive:true});this.document.addEventListener('click',onClick);this.document.addEventListener('keydown',onKey);desktop?.addEventListener('change',onDesktop);onScroll();});
    inject(DestroyRef).onDestroy(()=>{win?.removeEventListener('scroll',onScroll);this.document.removeEventListener('click',onClick);this.document.removeEventListener('keydown',onKey);desktop?.removeEventListener('change',onDesktop);this.closeMenu();root.style.scrollbarGutter=oldGutter;});
  }
  sectionHref(id:string){return this.host.marketingUrl('/#'+id);}
  verticalHref(slug:string){return this.host.marketingUrl('/'+encodeURIComponent(slug));}
  openMenu(event:Event){
    if(this.menuOpen())return;
    this.business.nativeElement.removeAttribute('open');
    this.opener=event.currentTarget as HTMLElement;
    this.oldOverflow=this.document.body.style.overflow;this.document.body.style.overflow='hidden';this.locked=true;
    this.menu.nativeElement.showModal();this.menuOpen.set(true);
  }
  closeMenu(){if(this.menu?.nativeElement.open)this.menu.nativeElement.close();this.finishClose();}
  finishClose(){
    this.menuOpen.set(false);
    if(!this.locked)return;
    this.document.body.style.overflow=this.oldOverflow;this.locked=false;
    if(this.opener?.isConnected){
      this.opener.focus({preventScroll:true});
      if(this.document.activeElement!==this.opener)this.menu.nativeElement.closest('app-marketing-nav')?.querySelector<HTMLElement>('.brand')?.focus({preventScroll:true});
    }
  }
  trapTab(event:KeyboardEvent){
    if(event.key!=='Tab' || !this.menu.nativeElement.open)return;
    const dialog=this.menu.nativeElement;
    const controls=[...dialog.querySelectorAll<HTMLElement>('a[href],button,input,select,textarea,[tabindex]')]
      .filter(e=>e.tabIndex>=0 && !e.hasAttribute('disabled') && !e.closest('[hidden]') && this.visible(e));
    const first=controls[0],last=controls.at(-1);
    if(!first||!last){event.preventDefault();dialog.focus();return;}
    if(event.shiftKey && (this.document.activeElement===first || !dialog.contains(this.document.activeElement))){event.preventDefault();last.focus();}
    else if(!event.shiftKey && (this.document.activeElement===last || !dialog.contains(this.document.activeElement))){event.preventDefault();first.focus();}
  }
  private visible(element:HTMLElement):boolean {
    if(typeof element.checkVisibility==='function')return element.checkVisibility({checkVisibilityCSS:true});
    for(let current:HTMLElement|null=element;current;current=current.parentElement){
      const style=getComputedStyle(current);
      if(style.display==='none'||style.visibility==='hidden')return false;
    }
    return true;
  }
  backdrop(event:MouseEvent){if(event.target!==this.menu.nativeElement)return;const r=this.menu.nativeElement.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)this.closeMenu();}
  choose(){this.business.nativeElement.removeAttribute('open');this.closeMenu();}
  signup(){this.offer.signup();this.choose();}
}
