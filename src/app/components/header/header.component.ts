import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar navbar-expand-lg navbar-light bg-white sticky-top shadow-sm">
      <div class="container">
        <a class="navbar-brand fw-bold text-primary-color" routerLink="/">
          <span class="fs-3">NailArt</span> <span class="text-secondary-color">Studio</span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav ms-auto">
            <li class="nav-item">
              <a class="nav-link mx-2" routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Inicio</a>
            </li>
            <li class="nav-item">
              <a class="nav-link mx-2" routerLink="/servicios" routerLinkActive="active">Servicios</a>
            </li>
            <li class="nav-item">
              <a class="nav-link mx-2" href="#nosotros">Nosotros</a>
            </li>
            <li class="nav-item">
              <a class="nav-link mx-2" routerLink="/agendar" routerLinkActive="active">Agendar Cita</a>
            </li>
            <li class="nav-item">
              <a class="nav-link mx-2" routerLink="/contacto" routerLinkActive="active">Contacto</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .text-primary-color { color: #e91e63; }
    .text-secondary-color { color: #333; }
    .nav-link {
      font-weight: 500;
      transition: color 0.3s;
    }
    .nav-link:hover, .nav-link.active {
      color: #e91e63 !important;
    }
  `]
})
export class HeaderComponent {}
