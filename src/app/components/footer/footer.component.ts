import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { PlatformContactService, PublicSupport } from '../../core/marketing/platform-contact.service';
import { supportSchedule } from '../../core/marketing/support-format';
import { Component, computed, inject, signal, effect } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';

import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.css'
})
export class FooterComponent {
  private readonly router = inject(Router);
  private readonly contactApi = inject(PlatformContactService);
  private readonly url = toSignal(this.router.events.pipe(filter(e => e instanceof NavigationEnd), map(() => this.router.url), startWith(this.router.navigated ? this.router.url : null)));
  readonly fluoraContact = computed(() => !this.tenantActivo() && !this.platformHost.isCustomTenantHost() && this.url() !== null && !/^\/e(?:\/|$)/.test((this.url() || '').split(/[?#]/)[0]));
  readonly support = signal<PublicSupport | null>(null);
  readonly schedule = supportSchedule;
  constructor() {
    effect(onCleanup => {
      if(!this.fluoraContact()) return;
      const subscription = this.contactApi.publicSupport().subscribe(value => this.support.set(value));
      onCleanup(() => subscription.unsubscribe());
    });
  }
  readonly currentYear = new Date().getFullYear();
  readonly tenantContext = inject(TenantContextService);
  readonly platformHost = inject(PlatformHostService);
  readonly tenantActivo = computed(() => this.tenantContext.activo());
  readonly tenantFooterColumnas = computed(() => 1
    + (this.tenantContext.footerMostrarNavegacion() ? 1 : 0)
    + (this.tenantContext.footerMostrarContacto() ? 1 : 0));
  readonly homeLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor() : '/');
  readonly servicesLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('servicios') : '/');
  readonly productsLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('productos') : '/');
  readonly bookingLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('agendar') : '/');
  readonly contactLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('contacto') : '/acceso');
  readonly accessHref = computed(() => this.platformHost.appUrl('/acceso'));
  readonly directoryHref = computed(() => this.platformHost.marketingUrl('/negocios'));
  readonly featuresHref = computed(() => this.platformHost.marketingUrl('/#producto'));
  readonly showcaseHref = computed(() => this.platformHost.marketingUrl('/#demostracion'));
  readonly reviewsHref = computed(() => this.platformHost.marketingUrl('/#faq'));
  readonly registerHref = computed(() => this.platformHost.marketingUrl('/registro'));
}
