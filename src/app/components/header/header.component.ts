import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
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
  readonly brandName = computed(() => this.tenantActivo() ? this.tenantContext.nombreComercial() : 'Refluora');
  readonly brandSubtitle = computed(() =>
    this.tenantActivo()
      ? (this.tenantContext.descripcionCorta() || 'Experiencia digital para tus clientes.')
      : 'CRM + Automatización + IA'
  );
  readonly fluoraHomeActiva = computed(() => !this.tenantActivo() && this.rutaActual() === '/');
  readonly navbarIntegradoHero = computed(() => this.fluoraHomeActiva() && !this.navbarCondensed());
  readonly navbarDesprendido = computed(() => this.fluoraHomeActiva() && this.navbarCondensed());
  readonly homeLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor() : '/');
  readonly servicesLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('servicios') : '/#funcionalidades');
  readonly contactLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('contacto') : '/#experiencias');
  readonly registerLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('registro') : '/registro');
  readonly ctaLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('agendar') : '/registro');
  readonly accessHref = computed(() => this.platformHost.appUrl('/acceso'));
  readonly featuresHref = computed(() => this.platformHost.marketingUrl('/#producto'));
  readonly workflowHref = computed(() => this.platformHost.marketingUrl('/#como-funciona'));
  readonly companiesHref = computed(() => this.platformHost.marketingUrl('/#beneficios'));
  readonly pricingHref = computed(() => this.platformHost.marketingUrl('/#faq'));
  readonly registerHref = computed(() => this.platformHost.marketingUrl('/registro'));

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
      });
  }

  @HostListener('window:scroll')
  onWindowScroll() {
    this.actualizarEstadoScroll();
  }

  logout() {
    this.authService.logout();
  }

  private actualizarEstadoScroll() {
    if (typeof window === 'undefined') {
      return;
    }

    this.navbarCondensed.set(window.scrollY > 72);
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
