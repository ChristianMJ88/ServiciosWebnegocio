import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const expectedPermissions = (route.data['permissions'] as string[] | undefined) ?? [];

  if (!authService.asegurarSesion()) {
    return router.createUrlTree(['/login']);
  }

  const currentPermissions = authService.sesionActual()?.permisos ?? [];
  if (expectedPermissions.length > 0 && expectedPermissions.some(permission => currentPermissions.includes(permission))) {
    return true;
  }

  return router.createUrlTree([authService.rutaPanel()]);
};
