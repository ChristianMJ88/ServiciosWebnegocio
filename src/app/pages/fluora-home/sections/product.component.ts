import { ChangeDetectionStrategy, Component, ElementRef, inject, signal } from '@angular/core';
import { HOME_CONTENT } from '../fluora-home.content';
@Component({selector:'app-home-product',standalone:true,templateUrl:'./product.component.html',styleUrl:'./product.component.css',changeDetection:ChangeDetectionStrategy.OnPush})
export class HomeProductComponent {
  readonly tabs=HOME_CONTENT.product;
  readonly selected=signal(0);
  private readonly element=inject<ElementRef<HTMLElement>>(ElementRef);
  select(index:number){this.selected.set(index);}
  keydown(event:KeyboardEvent,index:number){
    const key=event.key;
    if(!['ArrowLeft','ArrowRight','Home','End'].includes(key))return;
    event.preventDefault();
    const next=key==='Home'?0:key==='End'?this.tabs.length-1:(index+(key==='ArrowRight'?1:-1)+this.tabs.length)%this.tabs.length;
    this.select(next);
    const tab=this.element.nativeElement.querySelectorAll<HTMLButtonElement>('[role="tab"]')[next];
    tab?.focus({preventScroll:true});tab?.scrollIntoView({block:'nearest',inline:'nearest',behavior:'auto'});
  }
}
