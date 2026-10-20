import { hasMarketingTabs } from './core/marketing/marketing-route';
import { DOCUMENT, Location, isPlatformBrowser } from '@angular/common';
import { MarketingNavComponent } from './components/marketing-nav/marketing-nav.component';
import { SubscriptionAccessNoticeComponent } from './core/subscription/subscription-access-notice.component';
import { Component, ElementRef, PLATFORM_ID, computed, effect, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRouteSnapshot, NavigationCancel, NavigationCancellationCode, NavigationEnd, NavigationError, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { TENANT_PUBLIC_STYLES } from './core/styles/tenant-public-styles';

import { AuthService } from './core/auth/auth.service';
import { TenantContextService } from './core/tenant/tenant-context.service';
import { PlatformHostService } from './core/platform/platform-host.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, HeaderComponent, MarketingNavComponent, FooterComponent, SubscriptionAccessNoticeComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly browser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly document = inject(DOCUMENT);
  readonly tenantStyles = inject(TENANT_PUBLIC_STYLES);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly tenantContext = inject(TenantContextService);
  private readonly platformHost = inject(PlatformHostService);
  private readonly browserLocation=inject(Location);
  readonly publicPath=signal(this.normalizarUrl(this.browserLocation.path()||'/'));
  private readonly navigationRevision=signal(0);
  readonly esNavegacionMarketing=computed(()=>this.mostrarChromePublico() && !this.tenantContext.activo() && !this.platformHost.isCustomTenantHost() && !/^\/e(?:\/|$)/.test(this.publicPath()));
  readonly marketingBottomBar=computed(()=>{this.navigationRevision();return hasMarketingTabs(this.router.routerState.snapshot.root,this.publicPath());});
  readonly navegacionResuelta = signal(false);
  private readonly headerRef = viewChild('tenantHeader', { read: ElementRef });
  private readonly headerErrorRef = viewChild('tenantHeaderError', { read: ElementRef });
  readonly contenidoPendiente = computed(() =>
    this.mostrarChromePublico() && !this.esNavegacionMarketing()
    && !this.headerRef() && !this.headerErrorRef()
    && !(this.navegacionResuelta() && !this.tenantContext.activo())
  );
  protected readonly title = signal('ServiciosWebnegocio');
  esPanelInterno = signal(this.calcularEsPanelInterno());
  mostrarChromePublico = signal(this.calcularLayout() === 'public');
  publicReservationPage = signal(false);
  isChatOpen = signal(false);
  chatMsg = signal('');

  constructor() {
    effect(() => {
      if (this.mostrarChromePublico() && !this.esNavegacionMarketing()) void this.tenantStyles.ensure();
    });
    this.authService.sincronizarSesionPersistida();
    this.sincronizarTenantConRuta(this.publicPath());
    effect(() => {
      const tenant = this.tenantContext.actual();
      this.aplicarTemaTenant(tenant);
    });
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd | NavigationCancel | NavigationError => event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError),
        takeUntilDestroyed()
      )
      .subscribe(event => {
        if (event instanceof NavigationCancel && (event.code === NavigationCancellationCode.Redirect || event.code === NavigationCancellationCode.SupersededByNewNavigation)) return;
        this.navegacionResuelta.set(true);
        if (!(event instanceof NavigationEnd)) return;
        this.publicPath.set(this.normalizarUrl(this.router.url));
        this.navigationRevision.update(value=>value+1);
        this.sincronizarTenantConRuta(this.router.url);
        this.publicReservationPage.set(/\/(agendar|servicios|productos)(\/[^/]+)?$/.test(this.normalizarUrl(this.router.url)));
        this.esPanelInterno.set(this.calcularEsPanelInterno());
        this.mostrarChromePublico.set(this.calcularLayout() === 'public');
      });
  }

  recargarNavegacion(): void {
    if (this.browser) this.document.defaultView?.location.reload();
  }

  toggleChat() {
    this.isChatOpen.set(!this.isChatOpen());
  }

  clickChatOption(option: string) {
    if (option === 'precios') {
      this.chatMsg.set('Puedes ver todos nuestros precios en la sección de Servicios.');
    } else if (option === 'citas') {
      this.chatMsg.set('Haz clic en "Agendar Cita" en el menú para reservar tu espacio.');
    } else if (option === 'ubicacion') {
      this.chatMsg.set(this.tenantContext.direccion() || 'Consulta la sección de contacto para ver la ubicación.');
    }
  }

  private calcularEsPanelInterno(): boolean {
    return this.calcularLayout() === 'panel';
  }

  private calcularLayout(): 'public' | 'auth' | 'panel' {
    const rutaActiva = this.obtenerRutaActiva(this.router.routerState.snapshot.root);
    if (rutaActiva?.data?.['layout'] === 'panel') {
      return 'panel';
    }
    if (rutaActiva?.data?.['layout'] === 'auth') {
      return 'auth';
    }

    const urlNormalizada = this.normalizarUrl(this.router.navigated?this.router.url:this.publicPath());
    if(urlNormalizada==='/plataforma/acceso')return 'auth';
    if(urlNormalizada.startsWith('/plataforma'))return 'panel';
    if (['/admin', '/staff', '/recepcion', '/caja', '/mi-cuenta'].some(ruta => urlNormalizada.startsWith(ruta))) {
      return 'panel';
    }
    if (['/acceso', '/registro', '/invitacion/aceptar', '/verificar-correo', '/recuperar-contrasena'].some(ruta => urlNormalizada.startsWith(ruta))) {
      return 'auth';
    }
    return 'public';
  }

  private obtenerRutaActiva(snapshot: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
    let actual = snapshot;
    while (actual.firstChild) {
      actual = actual.firstChild;
    }
    return actual;
  }

  private normalizarUrl(url: string): string {
    const [sinQuery] = url.split('?');
    const [sinHash] = sinQuery.split('#');
    return sinHash.endsWith('/') && sinHash.length > 1 ? sinHash.slice(0, -1) : sinHash;
  }

  private sincronizarTenantConRuta(url: string) {
    const urlNormalizada = this.normalizarUrl(url);
    const esRutaTenant = urlNormalizada.startsWith('/e/');
    const esRutaPanel = ['/admin', '/staff', '/recepcion', '/caja', '/mi-cuenta'].some(ruta => urlNormalizada.startsWith(ruta));
    const esHostTenant = this.platformHost.isCustomTenantHost();

    if (!esRutaTenant && !esRutaPanel && !esHostTenant) {
      this.tenantContext.clearTenant();
    }
  }

  private aplicarTemaTenant(tenant: ReturnType<TenantContextService['actual']>) {
    if (!this.browser) {
      return;
    }

    const root = this.document.documentElement;
    const primario = tenant?.colorPrimario || '#2563eb';
    const secundario = tenant?.colorSecundario || '#0f766e';

    root.style.setProperty('--tenant-primary', primario);
    root.style.setProperty('--tenant-secondary', secundario);
    root.style.setProperty('--primary-pink', primario);
    root.style.setProperty('--accent-blue', secundario);
    root.style.setProperty('--primary-color', primario);
    root.style.setProperty('--primary-light', `${primario}1F`);
    root.style.setProperty('--primary-border', `${primario}55`);
    root.style.setProperty('--primary-shadow', `${primario}26`);
    root.style.setProperty('--tenant-heading-font', this.resolverFuenteCss(tenant?.fuenteTitulos || 'JAKARTA'));
    root.style.setProperty('--tenant-body-font', this.resolverFuenteCss(tenant?.fuenteCuerpo || 'INTER'));
    root.dataset['tenantTheme'] = this.normalizarTema(tenant?.tema);
  }

  private normalizarTema(valor?: string | null): string {
    const tema = (valor || '').trim().toUpperCase();
    return ['BASE', 'MINIMAL', 'ELEGANTE', 'VIBRANTE'].includes(tema) ? tema : 'BASE';
  }

  private resolverFuenteCss(fuente: string): string {
    switch ((fuente || 'INTER').toUpperCase()) {
      case 'JAKARTA':
        return '"Plus Jakarta Sans", Inter, "Segoe UI", sans-serif';
      case 'PLAYFAIR':
        return '"Playfair Display", Georgia, serif';
      case 'MANROPE':
        return 'Manrope, Inter, "Segoe UI", sans-serif';
      case 'SYSTEM':
        return 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
      case 'INTER':
      default:
        return 'Inter, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif';
    }
  }
}
