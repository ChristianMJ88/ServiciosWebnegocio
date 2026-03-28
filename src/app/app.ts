import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly title = signal('ServiciosWebnegocio');
  esPanelInterno = signal(this.calcularEsPanelInterno());
  isChatOpen = signal(false);
  chatMsg = signal('');

  constructor() {
    this.authService.sincronizarSesionPersistida();
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe(() => {
        this.esPanelInterno.set(this.calcularEsPanelInterno());
      });
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
      this.chatMsg.set('Estamos ubicados en Diagonal Benito Juarez #19. ¡Te esperamos!');
    }
  }

  private calcularEsPanelInterno(): boolean {
    const rutaActiva = this.obtenerRutaActiva(this.router.routerState.snapshot.root);
    if (rutaActiva?.data?.['layout'] === 'panel') {
      return true;
    }

    const urlNormalizada = this.normalizarUrl(this.router.url);
    return ['/admin', '/staff', '/recepcion', '/caja', '/mi-cuenta'].some(ruta => urlNormalizada.startsWith(ruta));
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
}
