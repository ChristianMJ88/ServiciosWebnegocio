import { DOCUMENT } from '@angular/common';
import { AfterViewInit, Directive, ElementRef, NgZone, OnDestroy, inject } from '@angular/core';
import { MarketingSectionsService } from './marketing-sections.service';

@Directive({selector:'[appMarketingSections]',standalone:true})
export class MarketingSectionsDirective implements AfterViewInit,OnDestroy {
  private readonly element=inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly document=inject(DOCUMENT);
  private readonly zone=inject(NgZone);
  private readonly state=inject(MarketingSectionsService);
  private observer?:IntersectionObserver;
  private resize?:()=>void;
  private frame?:number;
  private readonly ids=['inicio','como-funciona','producto','planes','faq'];

  ngAfterViewInit(){
    const win=this.document.defaultView;
    if(!win||typeof IntersectionObserver==='undefined')return;
    const elements=this.ids.map(id=>this.element.nativeElement.id===id?this.element.nativeElement:this.element.nativeElement.querySelector<HTMLElement>('#'+id)).filter((e):e is HTMLElement=>!!e);
    this.zone.runOutsideAngular(()=>{
      const observe=()=>{
        this.observer?.disconnect();
        const bar=this.document.querySelector<HTMLElement>('app-marketing-nav .marketing-topbar');
        const margin=Number.parseFloat(win.getComputedStyle(this.element.nativeElement).scrollMarginTop);
        const anchorLine=Number.isFinite(margin)&&margin>0?margin:(bar?.getBoundingClientRect().bottom||0);
        const line=Math.min(anchorLine+2,win.innerHeight-1);
        const publish=()=>{
          let current='';
          let closest=-Infinity;
          for(const element of elements){
            const top=element.getBoundingClientRect().top;
            if(top<=line+1&&top>=closest){current=element.id;closest=top;}
          }
          if(this.state.active()!==current)this.zone.run(()=>this.state.select(current));
        };
        // One-pixel observation strip below the app bar: sections switch at their headings.
        this.observer=new IntersectionObserver(publish,{rootMargin:`-${line}px 0px -${Math.max(0,win.innerHeight-line-1)}px 0px`,threshold:0});
        for(const element of elements)this.observer.observe(element);
        publish();
      };
      this.resize=()=>{
        if(this.frame!==undefined)return;
        this.frame=win.requestAnimationFrame(()=>{this.frame=undefined;observe();});
      };
      observe();
      win.addEventListener('resize',this.resize,{passive:true});
    });
  }
  ngOnDestroy(){
    this.observer?.disconnect();
    if(this.frame!==undefined)this.document.defaultView?.cancelAnimationFrame(this.frame);
    if(this.resize)this.document.defaultView?.removeEventListener('resize',this.resize);
    this.state.select('');
  }
}
