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
      this.originalTheme=this.meta.getTag('name="theme-color"')?.content??null;
      this.meta.updateTag({name:'theme-color',content:'#0A0F1A'});
    }
    if(safeArea && this.safeUsers++===0){
      this.originalViewport=this.meta.getTag('name="viewport"')?.content??null;
      const parts=(this.originalViewport||'width=device-width, initial-scale=1').split(',').map(s=>s.trim()).filter(s=>s && !/^viewport-fit\s*=/i.test(s));
      this.meta.updateTag({name:'viewport',content:[...parts,'viewport-fit=cover'].join(', ')});
    }
    let active=true;
    return ()=>{
      if(!active)return;active=false;
      if(safeArea && --this.safeUsers===0)this.restore('viewport',this.originalViewport);
      if(--this.users===0)this.restore('theme-color',this.originalTheme);
    };
  }
  private restore(name:string,content:string|null){
    if(content===null)this.meta.removeTag(`name="${name}"`);else this.meta.updateTag({name,content});
  }
}
