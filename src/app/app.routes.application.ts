import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { PERMISOS } from './core/auth/permissions';
import { roleGuard } from './core/auth/role.guard';

const redirectComponent = () => import('./pages/platform-redirect/platform-redirect.component')
  .then((m) => m.PlatformRedirectComponent);

export const privateApplicationRoutes: Routes = [
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
  {
    path: 'mi-cuenta', loadComponent: () => import('./pages/account/account.component').then((m) => m.AccountComponent),
    canActivate: [authGuard], data: { layout: 'panel' }
  },
  {
    path: 'staff', loadComponent: () => import('./pages/staff-dashboard/staff-dashboard.component').then((m) => m.StaffDashboardComponent),
    canActivate: [roleGuard], data: { permissions: [PERMISOS.staffPanel], layout: 'panel' }
  },
  { path: 'admin', redirectTo: 'admin/resumen', pathMatch: 'full' },
  {
    path: 'admin/:seccion', loadComponent: () => import('./pages/admin-dashboard/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
    canActivate: [roleGuard], data: { permissions: [PERMISOS.panelAdmin], layout: 'panel' }
  },
  {
    path: 'recepcion', loadComponent: () => import('./pages/recepcion-dashboard/recepcion-dashboard.component').then((m) => m.RecepcionDashboardComponent),
    canActivate: [roleGuard], data: { permissions: [PERMISOS.recepcionAcceso], layout: 'panel' }
  },
  {
    path: 'caja', loadComponent: () => import('./pages/caja-dashboard/caja-dashboard.component').then((m) => m.CajaDashboardComponent),
    canActivate: [roleGuard], data: { permissions: [PERMISOS.cajaAcceso], layout: 'panel' }
  }
];

export const applicationRoutes: Routes = [
  ...privateApplicationRoutes,
  { path: '', redirectTo: 'acceso', pathMatch: 'full' },
  { path: 'privacidad', loadComponent: redirectComponent, data: { destination: 'marketing' } },
  { path: 'terminos', loadComponent: redirectComponent, data: { destination: 'marketing' } },
  { path: 'eliminacion-de-datos', loadComponent: redirectComponent, data: { destination: 'marketing' } },
  { path: '**', redirectTo: 'acceso' }
];
