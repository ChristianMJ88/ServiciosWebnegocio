import { Routes } from '@angular/router';
import { privateApplicationRoutes } from './app.routes.application';
import { publicContentRoutes } from './app.routes.marketing';

// Desarrollo local integrado: conserva ambas superficies en un solo servidor.
export const routes: Routes = [
  ...publicContentRoutes,
  ...privateApplicationRoutes,
  { path: 'servicios', redirectTo: '', pathMatch: 'full' },
  { path: 'agendar', redirectTo: '', pathMatch: 'full' },
  { path: 'contacto', redirectTo: '', pathMatch: 'full' },
  { path: '**', redirectTo: '' }
];
