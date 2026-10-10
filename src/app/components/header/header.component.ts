import { MarketingTrackingService } from '../../core/marketing/marketing-tracking.service';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  readonly authService = inject(AuthService);
  readonly tenantContext = inject(TenantContextService);
  readonly platformHost = inject(PlatformHostService);
  private readonly router = inject(Router);
  readonly autenticadoVisible = computed(() => this.authService.autenticado());
  readonly rutaPanelVisible = computed(() => this.authService.rutaPanel());
  readonly tenantActivo = computed(() => this.tenantContext.activo());
  readonly rutaActual = signal(this.normalizarUrl(this.router.url));
  readonly navbarCondensed = signal(false);
  readonly mobileMenuOpen = signal(false);
  readonly brandName = computed(() => this.tenantActivo() ? this.tenantContext.nombreComercial() : 'Fluora');
  readonly brandSubtitle = computed(() =>
    this.tenantActivo()
      ? this.tenantContext.descripcionCorta()
      : 'Agenda para tu negocio'
  );
  readonly marketingLandingActiva = computed(() => { this.rutaActual(); return this.router.routerState.snapshot.root.firstChild?.data['marketingLanding'] === true; });
  readonly fluoraHomeActiva = computed(() => !this.tenantActivo() && (this.rutaActual() === '/' || this.router.routerState.snapshot.root.firstChild?.data['marketingLanding'] === true));
  readonly tenantHomeActiva = computed(() => this.tenantActivo() && this.rutaActual() === this.homeLink());
  readonly headerSobreHero = computed(() => this.fluoraHomeActiva() || this.tenantHomeActiva());
  readonly navbarIntegradoHero = computed(() => this.fluoraHomeActiva() && !this.navbarCondensed());
  readonly navbarDesprendido = computed(() => this.tenantHomeActiva() || (this.fluoraHomeActiva() && this.navbarCondensed()));
  readonly menuDesprendido = computed(() => this.mobileMenuOpen() && this.navbarDesprendido());
  readonly menuSolido = computed(() => this.tenantActivo() && this.tenantContext.menuEstilo() === 'SOLIDO');
  readonly homeLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor() : '/');
  readonly servicesLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('servicios') : '/#funcionalidades');
  readonly productsLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('productos') : '/#funcionalidades');
  readonly contactLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('contacto') : '/#experiencias');
  readonly registerLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('registro') : '/registro');
  readonly ctaLink = computed(() => this.tenantActivo()
    ? this.tenantContext.actionRoute(this.tenantContext.menuAccionCta())
    : '/registro');
  readonly ctaWhatsapp = computed(() => this.tenantActivo()
    && this.tenantContext.menuAccionCta() === 'WHATSAPP'
    && this.tenantContext.whatsappHabilitado());
  readonly accessHref = computed(() => this.platformHost.appUrl('/acceso'));
  readonly marketingHomeHref = computed(() => this.platformHost.marketingUrl('/'));
  readonly featuresHref = computed(() => this.platformHost.marketingUrl('/#producto'));
  readonly workflowHref = computed(() => this.platformHost.marketingUrl('/#como-funciona'));
  readonly companiesHref = computed(() => this.platformHost.marketingUrl('/#beneficios'));
  readonly pricingHref = computed(() => this.platformHost.marketingUrl('/#planes'));
  readonly faqHref = computed(() => this.platformHost.marketingUrl('/#faq'));
  private readonly marketingTracking = inject(MarketingTrackingService);
  readonly registerHref = computed(() => this.marketingLandingActiva()
    ? this.marketingTracking.registrationUrl(this.platformHost.appUrl('/registro'))
    : this.platformHost.appUrl('/registro'));
  onMarketingSignup(): void {
    if (this.marketingLandingActiva()) this.marketingTracking.emit('signup_click');
    this.onNavAction();
  }

  constructor() {
    this.actualizarEstadoScroll();
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe((event) => {
        this.rutaActual.set(this.normalizarUrl(event.urlAfterRedirects));
        this.actualizarEstadoScroll();
        this.cerrarMenuMovil();
      });
  }

  @HostListener('window:scroll')
  onWindowScroll() {
    this.actualizarEstadoScroll();
  }

  @HostListener('window:resize')
  onWindowResize() {
    if (typeof window !== 'undefined' && window.innerWidth >= 992) {
      this.cerrarMenuMovil();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    this.cerrarMenuMovil();
  }

  logout() {
    this.cerrarMenuMovil();
    this.authService.logout();
  }

  onNavAction() {
    this.cerrarMenuMovil();
  }

  alternarMenuMovil() {
    this.mobileMenuOpen.update(abierto => !abierto);
  }

  private actualizarEstadoScroll() {
    if (typeof window === 'undefined') {
      return;
    }

    this.navbarCondensed.set(window.scrollY > 72);
  }

  private cerrarMenuMovil() {
    this.mobileMenuOpen.set(false);
  }

  private normalizarUrl(url: string): string {
    const [sinQuery] = url.split('?');
    const [sinHash] = sinQuery.split('#');

    if (!sinHash) {
      return '/';
    }

    return sinHash.endsWith('/') && sinHash.length > 1 ? sinHash.slice(0, -1) : sinHash;
  }
}
