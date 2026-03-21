import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const expectedRoles = (route.data['roles'] as string[] | undefined) ?? [];

  if (!authService.asegurarSesion()) {
    return router.createUrlTree(['/login']);
  }

  const currentRoles = authService.sesionActual()?.roles ?? [];
  if (expectedRoles.some(role => currentRoles.includes(role))) {
    return true;
  }

  return router.createUrlTree([authService.rutaPanel()]);
};
