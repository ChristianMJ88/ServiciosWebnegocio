import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { CompanyCapabilitiesService } from './company-capabilities.service';

export const moduleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const capabilities = inject(CompanyCapabilitiesService);
  const router = inject(Router);
  const requiredModule = route.data['module'] as string | undefined;
  if (!requiredModule) return true;
  return capabilities.load().pipe(
    map(() => capabilities.hasModule(requiredModule)
      ? true
      : router.createUrlTree(['/admin/suscripcion'], { queryParams: { modulo: requiredModule } })),
    catchError(() => of(router.createUrlTree(['/admin/suscripcion'])))
  );
};
