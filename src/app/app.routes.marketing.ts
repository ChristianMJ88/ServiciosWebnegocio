import { Routes } from '@angular/router';
import { tenantHostMatch } from './core/tenant/tenant-host.matchers';
import { tenantHostResolver } from './core/tenant/tenant-host.resolver';
import { tenantResolver } from './core/tenant/tenant.resolver';

const redirectComponent = () => import('./pages/platform-redirect/platform-redirect.component')
  .then((m) => m.PlatformRedirectComponent);

export const publicContentRoutes: Routes = [
  {
    path: '', canMatch: [tenantHostMatch], resolve: { tenant: tenantHostResolver },
    loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent)
  },
  {
    path: 'servicios', canMatch: [tenantHostMatch], resolve: { tenant: tenantHostResolver },
    loadComponent: () => import('./pages/services/services.component').then((m) => m.ServicesComponent)
  },
  {
    path: 'agendar', canMatch: [tenantHostMatch], resolve: { tenant: tenantHostResolver },
    loadComponent: () => import('./pages/booking/booking.component').then((m) => m.BookingComponent)
  },
  {
    path: 'contacto', canMatch: [tenantHostMatch], resolve: { tenant: tenantHostResolver },
    loadComponent: () => import('./pages/contact/contact.component').then((m) => m.ContactComponent)
  },
  {
    path: 'registro', canMatch: [tenantHostMatch], resolve: { tenant: tenantHostResolver },
    loadComponent: () => import('./pages/register/register.component').then((m) => m.RegisterComponent)
  },
  { path: '', loadComponent: () => import('./pages/fluora-home/fluora-home.component').then((m) => m.FluoraHomeComponent) },
  {
    path: 'privacidad',
    loadComponent: () => import('./pages/legal/legal-page.component').then((m) => m.LegalPageComponent),
    data: { legalDocument: 'privacy' }
  },
  {
    path: 'terminos',
    loadComponent: () => import('./pages/legal/legal-page.component').then((m) => m.LegalPageComponent),
    data: { legalDocument: 'terms' }
  },
  {
    path: 'eliminacion-de-datos',
    loadComponent: () => import('./pages/legal/legal-page.component').then((m) => m.LegalPageComponent),
    data: { legalDocument: 'deletion' }
  },
  { path: 'demo', redirectTo: '', pathMatch: 'full' },
  {
    path: 'e/:slug', resolve: { tenant: tenantResolver },
    children: [
      { path: '', loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent) },
      { path: 'servicios', loadComponent: () => import('./pages/services/services.component').then((m) => m.ServicesComponent) },
      { path: 'agendar', loadComponent: () => import('./pages/booking/booking.component').then((m) => m.BookingComponent) },
      { path: 'login', loadComponent: redirectComponent, data: { destination: 'app', path: '/acceso' } },
      { path: 'registro', loadComponent: () => import('./pages/register/register.component').then((m) => m.RegisterComponent) },
      { path: 'contacto', loadComponent: () => import('./pages/contact/contact.component').then((m) => m.ContactComponent) }
    ]
  }
];

export const marketingRoutes: Routes = [
  ...publicContentRoutes,
  { path: 'registro', loadComponent: redirectComponent, data: { destination: 'app', path: '/registro' } },
  { path: 'acceso', loadComponent: redirectComponent, data: { destination: 'app', path: '/acceso' } },
  { path: 'login', loadComponent: redirectComponent, data: { destination: 'app', path: '/acceso' } },
  { path: 'invitacion/aceptar', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'verificar-correo', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'recuperar-contrasena', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'recuperar-contrasena/confirmar', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'mi-cuenta', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'staff', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'admin', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'admin/:seccion', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'recepcion', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'caja', loadComponent: redirectComponent, data: { destination: 'app' } },
  { path: 'servicios', redirectTo: '', pathMatch: 'full' },
  { path: 'agendar', redirectTo: '', pathMatch: 'full' },
  { path: 'contacto', redirectTo: '', pathMatch: 'full' },
  { path: '**', redirectTo: '' }
];
