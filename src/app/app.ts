import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('ServiciosWebnegocio');
  isChatOpen = signal(false);
  chatMsg = signal('');

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
}
