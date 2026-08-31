import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { PERMISOS } from './core/auth/permissions';
import { tenantResolver } from './core/tenant/tenant.resolver';
import { tenantHostResolver } from './core/tenant/tenant-host.resolver';
import { tenantHostMatch } from './core/tenant/tenant-host.matchers';

export const routes: Routes = [
  {
    path: '',
    canMatch: [tenantHostMatch],
    resolve: { tenant: tenantHostResolver },
    loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent)
  },
  {
    path: 'servicios',
    canMatch: [tenantHostMatch],
    resolve: { tenant: tenantHostResolver },
    loadComponent: () => import('./pages/services/services.component').then((m) => m.ServicesComponent)
  },
  {
    path: 'agendar',
    canMatch: [tenantHostMatch],
    resolve: { tenant: tenantHostResolver },
    loadComponent: () => import('./pages/booking/booking.component').then((m) => m.BookingComponent)
  },
  {
    path: 'contacto',
    canMatch: [tenantHostMatch],
    resolve: { tenant: tenantHostResolver },
    loadComponent: () => import('./pages/contact/contact.component').then((m) => m.ContactComponent)
  },
  {
    path: 'registro',
    canMatch: [tenantHostMatch],
    resolve: { tenant: tenantHostResolver },
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
  { path: 'acceso', loadComponent: () => import('./pages/fluora-access/fluora-access.component').then((m) => m.FluoraAccessComponent) },
  { path: 'invitacion/aceptar', loadComponent: () => import('./pages/accept-user-invitation/accept-user-invitation.component').then((m) => m.AcceptUserInvitationComponent) },
  { path: 'verificar-correo', loadComponent: () => import('./pages/verify-email/verify-email.component').then((m) => m.VerifyEmailComponent) },
  { path: 'recuperar-contrasena', loadComponent: () => import('./pages/password-recovery/password-recovery.component').then((m) => m.PasswordRecoveryComponent) },
  { path: 'recuperar-contrasena/confirmar', loadComponent: () => import('./pages/password-recovery/password-recovery.component').then((m) => m.PasswordRecoveryComponent) },
  {
    path: 'registro',
    loadComponent: () => import('./pages/fluora-register-company/fluora-register-company.component').then((m) => m.FluoraRegisterCompanyComponent)
  },
  { path: 'login', redirectTo: 'acceso', pathMatch: 'full' },
  { path: 'demo', redirectTo: '', pathMatch: 'full' },
  {
    path: 'e/:slug',
    resolve: { tenant: tenantResolver },
    children: [
      { path: '', loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent) },
      { path: 'servicios', loadComponent: () => import('./pages/services/services.component').then((m) => m.ServicesComponent) },
      { path: 'agendar', loadComponent: () => import('./pages/booking/booking.component').then((m) => m.BookingComponent) },
      { path: 'login', redirectTo: '/acceso', pathMatch: 'full' },
      { path: 'registro', loadComponent: () => import('./pages/register/register.component').then((m) => m.RegisterComponent) },
      { path: 'contacto', loadComponent: () => import('./pages/contact/contact.component').then((m) => m.ContactComponent) }
    ]
  },
  { path: 'servicios', redirectTo: '', pathMatch: 'full' },
  { path: 'agendar', redirectTo: '', pathMatch: 'full' },
  {
    path: 'mi-cuenta',
    loadComponent: () => import('./pages/account/account.component').then((m) => m.AccountComponent),
    canActivate: [authGuard],
    data: { layout: 'panel' }
  },
  {
    path: 'staff',
    loadComponent: () => import('./pages/staff-dashboard/staff-dashboard.component').then((m) => m.StaffDashboardComponent),
    canActivate: [roleGuard],
    data: { permissions: [PERMISOS.staffPanel], layout: 'panel' }
  },
  {
    path: 'admin',
    redirectTo: 'admin/resumen',
    pathMatch: 'full'
  },
  {
    path: 'admin/:seccion',
    loadComponent: () => import('./pages/admin-dashboard/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
    canActivate: [roleGuard],
    data: { permissions: [PERMISOS.panelAdmin], layout: 'panel' }
  },
  {
    path: 'recepcion',
    loadComponent: () => import('./pages/recepcion-dashboard/recepcion-dashboard.component').then((m) => m.RecepcionDashboardComponent),
    canActivate: [roleGuard],
    data: { permissions: [PERMISOS.recepcionAcceso], layout: 'panel' }
  },
  {
    path: 'caja',
    loadComponent: () => import('./pages/caja-dashboard/caja-dashboard.component').then((m) => m.CajaDashboardComponent),
    canActivate: [roleGuard],
    data: { permissions: [PERMISOS.cajaAcceso], layout: 'panel' }
  },
  { path: 'contacto', redirectTo: '', pathMatch: 'full' },
  { path: '**', redirectTo: '' }
];
