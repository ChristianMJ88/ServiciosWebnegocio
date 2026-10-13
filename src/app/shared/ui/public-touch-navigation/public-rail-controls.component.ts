import { Component, Input, OnDestroy, signal } from '@angular/core';

@Component({
  selector: 'app-public-rail-controls', standalone: true,
  template: `@if (overflow()) { <div class="rail-controls"><span>Desliza para explorar</span><div><button type="button" aria-label="Ver anteriores" [disabled]="atStart()" (click)="move(-1)"><i class="bi bi-chevron-left" aria-hidden="true"></i></button><button type="button" aria-label="Ver siguientes" [disabled]="atEnd()" (click)="move(1)"><i class="bi bi-chevron-right" aria-hidden="true"></i></button></div></div> }`,
  styles: [`:host{display:block}.rail-controls{display:flex;align-items:center;justify-content:space-between;gap:1rem;margin-bottom:.75rem;color:#64717d;font-size:.8rem}.rail-controls>div{display:flex;gap:.5rem}button{display:grid;place-items:center;width:44px;height:44px;border:1px solid color-mix(in srgb,var(--tenant-primary) 20%,#e7ebef);border-radius:50%;background:white;color:var(--tenant-primary)}button:disabled{opacity:.35}button:focus-visible{outline:2px solid var(--tenant-primary);outline-offset:3px}`]
})
export class PublicRailControlsComponent implements OnDestroy {
  readonly overflow = signal(false);
  readonly atStart = signal(true);
  readonly atEnd = signal(false);
  private element?: HTMLElement;
  private resize?: ResizeObserver;
  private changes?: MutationObserver;
  private readonly update = () => {
    const el = this.element;
    if (!el) return;
    this.overflow.set(el.scrollWidth > el.clientWidth + 2);
    this.atStart.set(el.scrollLeft <= 2);
    this.atEnd.set(el.scrollLeft + el.clientWidth >= el.scrollWidth - 2);
  };
  @Input({ required: true }) set target(el: HTMLElement) {
    this.disconnect(); this.element = el;
    this.resize = new ResizeObserver(this.update); this.resize.observe(el);
    const observeCards = () => { if (this.element !== el) return; Array.from(el.children).forEach(card => this.resize?.observe(card)); this.update(); };
    this.changes = new MutationObserver(observeCards); this.changes.observe(el, {childList:true});
    el.addEventListener('scroll', this.update, {passive:true});
    queueMicrotask(observeCards);
  }
  move(direction: number): void {
    this.element?.scrollBy({left: direction * this.element.clientWidth * .85,
      behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'});
  }
  private disconnect(): void { this.resize?.disconnect(); this.changes?.disconnect(); this.element?.removeEventListener('scroll', this.update); this.element = undefined; this.resize = undefined; this.changes = undefined; }
  ngOnDestroy(): void { this.disconnect(); }
}
