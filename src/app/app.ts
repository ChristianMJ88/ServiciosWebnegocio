import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
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
  esPanelInterno = signal(this.calcularEsPanelInterno(this.router.url));
  isChatOpen = signal(false);
  chatMsg = signal('');

  constructor() {
    this.authService.sincronizarSesionPersistida();
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe(evento => {
        this.esPanelInterno.set(this.calcularEsPanelInterno(evento.urlAfterRedirects));
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
      this.chatMsg.set('Estamos ubicados en Calle Principal #123. ¡Te esperamos!');
    }
  }

  private calcularEsPanelInterno(url: string): boolean {
    return ['/admin', '/staff', '/mi-cuenta'].some(ruta => url.startsWith(ruta));
  }
}
