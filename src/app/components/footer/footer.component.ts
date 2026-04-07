import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.css'
})
export class FooterComponent {
  readonly tenantContext = inject(TenantContextService);
  readonly platformHost = inject(PlatformHostService);
  readonly tenantActivo = computed(() => this.tenantContext.activo());
  readonly homeLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor() : '/');
  readonly servicesLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('servicios') : '/');
  readonly bookingLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('agendar') : '/');
  readonly contactLink = computed(() => this.tenantActivo() ? this.tenantContext.routeFor('contacto') : '/acceso');
  readonly accessHref = computed(() => this.platformHost.appUrl('/acceso'));
  readonly featuresHref = computed(() => this.platformHost.marketingUrl('/#funcionalidades'));
  readonly showcaseHref = computed(() => this.platformHost.marketingUrl('/#empresas'));
  readonly reviewsHref = computed(() => this.platformHost.marketingUrl('/#resenas'));
  readonly registerHref = computed(() => this.platformHost.marketingUrl('/registro'));
}
