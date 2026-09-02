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
      ? (this.tenantContext.descripcionCorta() || 'Experiencia digital para tus clientes.')
      : 'Agenda para tu negocio'
  );
  readonly fluoraHomeActiva = computed(() => !this.tenantActivo() && this.rutaActual() === '/');
  readonly tenantHomeActiva = computed(() => this.tenantActivo() && this.rutaActual() === this.homeLink());
  readonly headerSobreHero = computed(() => this.fluoraHomeActiva() || this.tenantHomeActiva());
  readonly navbarIntegradoHero = computed(() => this.fluoraHomeActiva() && !this.navbarCondensed());
  readonly navbarDesprendido = computed(() => this.tenantHomeActiva() || (this.fluoraHomeActiva() && this.navbarCondensed()));
  readonly menuDesprendido = computed(() => this.mobileMenuOpen() && this.navbarDesprendido());
  readonly homeLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor() : '/');
  readonly servicesLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('servicios') : '/#funcionalidades');
  readonly contactLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('contacto') : '/#experiencias');
  readonly registerLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('registro') : '/registro');
  readonly ctaLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('agendar') : '/registro');
  readonly accessHref = computed(() => this.platformHost.appUrl('/acceso'));
  readonly marketingHomeHref = computed(() => this.platformHost.marketingUrl('/'));
  readonly featuresHref = computed(() => this.platformHost.marketingUrl('/#producto'));
  readonly workflowHref = computed(() => this.platformHost.marketingUrl('/#como-funciona'));
  readonly companiesHref = computed(() => this.platformHost.marketingUrl('/#beneficios'));
  readonly pricingHref = computed(() => this.platformHost.marketingUrl('/#planes'));
  readonly faqHref = computed(() => this.platformHost.marketingUrl('/#faq'));
  readonly registerHref = computed(() => this.platformHost.appUrl('/registro'));

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
