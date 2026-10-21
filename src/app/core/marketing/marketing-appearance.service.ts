import { Injectable, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';
/** Leases keep marketing metadata temporary, independently of the native navigation safe area. */
@Injectable({providedIn:'root'})
export class MarketingAppearanceService {
  private readonly meta=inject(Meta);
  private users=0;
  private safeUsers=0;
  private originalTheme:string|null=null;
  private originalViewport:string|null=null;
  activate(safeArea=false):()=>void {
    if(this.users++===0){
      this.originalTheme=this.original('theme-color');
      this.mark(this.meta.updateTag({name:'theme-color',content:'#0A0F1A'}),this.originalTheme);
    }
    if(safeArea && this.safeUsers++===0){
      this.originalViewport=this.original('viewport');
      const parts=(this.originalViewport||'width=device-width, initial-scale=1').split(',').map(s=>s.trim()).filter(s=>s && !/^viewport-fit\s*=/i.test(s));
      this.mark(this.meta.updateTag({name:'viewport',content:[...parts,'viewport-fit=cover'].join(', ')}),this.originalViewport);
    }
    let active=true;
    return ()=>{
      if(!active)return;active=false;
      if(safeArea && --this.safeUsers===0)this.restore('viewport',this.originalViewport);
      if(--this.users===0)this.restore('theme-color',this.originalTheme);
    };
  }
  private original(name:string):string|null {
    const tag=this.meta.getTag(`name="${name}"`);
    return tag?.getAttribute('data-fl-lease')==='created'?null:tag?.getAttribute('data-fl-original')??tag?.content??null;
  }
  private mark(tag:HTMLMetaElement|null,original:string|null):void {
    if(!tag)return;
    if(original===null){tag.setAttribute('data-fl-lease','created');tag.removeAttribute('data-fl-original');}
    else{tag.setAttribute('data-fl-original',original);tag.removeAttribute('data-fl-lease');}
  }
  private restore(name:string,content:string|null){
    if(content===null)this.meta.removeTag(`name="${name}"`);
    else{const tag=this.meta.updateTag({name,content});tag?.removeAttribute('data-fl-original');tag?.removeAttribute('data-fl-lease');}
  }
}
