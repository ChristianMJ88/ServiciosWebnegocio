import { AfterViewInit, Component, Input, OnDestroy, signal } from '@angular/core';

@Component({
  selector: 'app-public-section-navigation', standalone: true,
  template: `@if (sections().length > 1) { <nav aria-label="Explorar secciones del negocio">@for (section of sections(); track section.element) { <button type="button" (click)="go(section.element)">{{ section.title }}</button> }</nav> }`,
  styles: [`:host{display:block;position:sticky;top:5rem;z-index:15;order:-1;background:color-mix(in srgb,var(--tenant-primary) 3%,white);border-block:1px solid #e7ebef}nav{display:flex;gap:.5rem;overflow-x:auto;max-width:1200px;margin:auto;padding:.65rem 1rem;scrollbar-width:thin}button{flex:0 0 auto;min-height:44px;border:1px solid #e7ebef;background:white;color:#26323d;border-radius:99px;padding:.5rem 1rem;font:inherit;font-size:.85rem;font-weight:600;max-width:260px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}button:focus-visible{outline:2px solid var(--tenant-primary);outline-offset:2px}@media(min-width:992px){:host{position:relative;top:auto}}`]
})
export class PublicSectionNavigationComponent implements AfterViewInit, OnDestroy {
  @Input({required:true}) root!: HTMLElement;
  readonly sections = signal<{element:HTMLElement;title:string}[]>([]);
  private observer?: MutationObserver;
  private refresh = () => {
    // Only navigate sections actually rendered from the tenant's configuration.
    const sections = Array.from(this.root.querySelectorAll<HTMLElement>('.home-section'))
      .map(element => ({element,title:element.querySelector('h2')?.textContent?.trim() ?? ''}))
      .filter(section => section.title)
      .sort((a,b) => Number(a.element.style.order) - Number(b.element.style.order));
    const current = this.sections();
    if (sections.length !== current.length || sections.some((item,index) => item.element !== current[index]?.element || item.title !== current[index]?.title)) this.sections.set(sections);
  };
  ngAfterViewInit(): void {
    this.observer = new MutationObserver(this.refresh);
    this.observer.observe(this.root,{childList:true,subtree:true,characterData:true});
    queueMicrotask(this.refresh);
  }
  go(element: HTMLElement): void {
    element.scrollIntoView({block:'start',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'});
  }
  ngOnDestroy(): void { this.observer?.disconnect(); }
}
