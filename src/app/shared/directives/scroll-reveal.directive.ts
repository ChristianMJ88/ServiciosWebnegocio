import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Directive, ElementRef, Input, OnDestroy, PLATFORM_ID, Renderer2, inject } from '@angular/core';

type RevealDirection = 'up' | 'left' | 'right';

@Directive({
  selector: '[appScrollReveal]',
  standalone: true
})
export class ScrollRevealDirective implements AfterViewInit, OnDestroy {
  @Input('appScrollReveal') direction: RevealDirection = 'up';
  @Input() revealDelay = 0;

  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly renderer = inject(Renderer2);
  private readonly platformId = inject(PLATFORM_ID);
  private observer?: IntersectionObserver;

  ngAfterViewInit(): void {
    const element = this.elementRef.nativeElement;
    this.renderer.addClass(element, 'scroll-reveal');
    this.renderer.addClass(element, `scroll-reveal--${this.direction}`);

    if (this.revealDelay > 0) {
      this.renderer.setStyle(element, 'transition-delay', `${this.revealDelay}ms`);
    }

    if (!isPlatformBrowser(this.platformId)) {
      this.renderer.addClass(element, 'is-visible');
      return;
    }

    this.observer = new IntersectionObserver(
      entries => {
        for (const entry of entries) {
          if (!entry.isIntersecting) {
            continue;
          }

          this.renderer.addClass(element, 'is-visible');
          this.observer?.unobserve(element);
        }
      },
      {
        threshold: 0.18,
        rootMargin: '0px 0px -12% 0px'
      }
    );

    this.observer.observe(element);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
