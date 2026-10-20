import { inject } from '@angular/core';
import { PlatformHostService } from './core/platform/platform-host.service';
import { Routes } from '@angular/router';
import { tenantHostMatch } from './core/tenant/tenant-host.matchers';
import { tenantHostResolver } from './core/tenant/tenant-host.resolver';
import { tenantResolver } from './core/tenant/tenant.resolver';
import { tenantPublicStylesResolver } from './core/styles/tenant-public-styles';

const tenantHostPublicResolvers = { tenant: tenantHostResolver, estilos: tenantPublicStylesResolver };

const redirectComponent = () => import('./pages/platform-redirect/platform-redirect.component')
  .then((m) => m.PlatformRedirectComponent);

export const publicContentRoutes: Routes = [
  { path: 'politicas', canMatch: [tenantHostMatch], resolve: tenantHostPublicResolvers, loadComponent: () => import('./pages/business-policy/business-policy.component').then(m => m.BusinessPolicyComponent) },
  { path: 'negocios', canMatch: [() => inject(PlatformHostService).isKnownPlatformHost()],
    loadComponent: () => import('./pages/marketplace/marketplace.component').then(m => m.MarketplaceComponent) },
  {
    path: '', canMatch: [tenantHostMatch], resolve: tenantHostPublicResolvers,
    loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent)
  },
  {
    path: 'servicios', canMatch: [tenantHostMatch], resolve: tenantHostPublicResolvers,
    loadComponent: () => import('./pages/services/services.component').then((m) => m.ServicesComponent)
  },
  {
    path: 'productos', canMatch: [tenantHostMatch], resolve: tenantHostPublicResolvers,
    loadComponent: () => import('./pages/products/products.component').then((m) => m.ProductsComponent)
  },
  {
    path: 'productos/:productoSlug', canMatch: [tenantHostMatch], resolve: tenantHostPublicResolvers,
    loadComponent: () => import('./pages/products/products.component').then((m) => m.ProductsComponent)
  },
  {
    path: 'checkout', canMatch: [tenantHostMatch], resolve: tenantHostPublicResolvers,
    loadComponent: () => import('./pages/checkout/checkout.component').then((m) => m.CheckoutComponent)
  },
  {
    path: 'agendar', canMatch: [tenantHostMatch], resolve: tenantHostPublicResolvers,
    loadComponent: () => import('./pages/booking/booking.component').then((m) => m.BookingComponent)
  },
  {
    path: 'contacto', canMatch: [tenantHostMatch], resolve: tenantHostPublicResolvers,
    loadComponent: () => import('./pages/contact/contact.component').then((m) => m.ContactComponent)
  },
  {
    path: 'registro', canMatch: [tenantHostMatch], resolve: tenantHostPublicResolvers,
    loadComponent: () => import('./pages/register/register.component').then((m) => m.RegisterComponent)
  },
  ...['barberias', 'salones', 'esteticas', 'unas', 'spa'].map(vertical => ({
    path: vertical, canMatch: [() => inject(PlatformHostService).isKnownPlatformHost()],
    loadComponent: () => import('./pages/fluora-vertical/fluora-vertical.component').then(m => m.FluoraVerticalComponent),
    data: { vertical, marketingLanding: true }
  })),
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
    path: 'e/:slug', resolve: { tenant: tenantResolver, estilos: tenantPublicStylesResolver },
    children: [
      { path: 'politicas', loadComponent: () => import('./pages/business-policy/business-policy.component').then(m => m.BusinessPolicyComponent) },
      { path: '', loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent) },
      { path: 'servicios', loadComponent: () => import('./pages/services/services.component').then((m) => m.ServicesComponent) },
      { path: 'productos', loadComponent: () => import('./pages/products/products.component').then((m) => m.ProductsComponent) },
      { path: 'productos/:productoSlug', loadComponent: () => import('./pages/products/products.component').then((m) => m.ProductsComponent) },
      { path: 'checkout', loadComponent: () => import('./pages/checkout/checkout.component').then((m) => m.CheckoutComponent) },
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
  { path: 'productos', redirectTo: '', pathMatch: 'full' },
  { path: 'agendar', redirectTo: '', pathMatch: 'full' },
  { path: 'contacto', redirectTo: '', pathMatch: 'full' },
  { path: '**', redirectTo: '' }
];
