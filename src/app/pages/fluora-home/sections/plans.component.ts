import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, ViewChild, computed, inject, signal } from '@angular/core';
import { MarketingOfferService } from '../../../core/marketing/marketing-offer.service';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';
@Component({selector:'app-home-plans',standalone:true,imports:[MoneyDisplayPipe],templateUrl:'./plans.component.html',styleUrl:'./plans.component.css',changeDetection:ChangeDetectionStrategy.OnPush})
export class HomePlansComponent {
  readonly offer=inject(MarketingOfferService);
  private rail?:ElementRef<HTMLElement>;
  private observer?:ResizeObserver;
  readonly canScroll=signal(false);
  private readonly firstRailMeasured=signal(false);
  readonly layoutReady=computed(()=>!this.offer.loading()&&(this.offer.failed()||!this.offer.plans().length||this.firstRailMeasured()));
  @ViewChild('rail') set railTarget(value:ElementRef<HTMLElement>|undefined){
    this.observer?.disconnect();this.rail=value;
    this.firstRailMeasured.set(false);
    if(!value){this.canScroll.set(false);return;}
    // Commit geometry and readiness together: an anchor must see the controls in DOM.
    const update=()=>{if(this.rail===value){this.canScroll.set(value.nativeElement.scrollWidth>value.nativeElement.clientWidth+1);this.firstRailMeasured.set(true);}};
    if(typeof ResizeObserver!=='undefined'){this.observer=new ResizeObserver(update);this.observer.observe(value.nativeElement);}
    if(!this.observer)queueMicrotask(update);
  }
  constructor(){inject(DestroyRef).onDestroy(()=>this.observer?.disconnect());}
  move(direction:number){const element=this.rail?.nativeElement;const card=element?.querySelector('article');if(element&&card)element.scrollBy({left:direction*(card.getBoundingClientRect().width+(Number.parseFloat(getComputedStyle(element).columnGap)||0)),behavior:globalThis.matchMedia?.('(prefers-reduced-motion:reduce)').matches?'auto':'smooth'});}
}
