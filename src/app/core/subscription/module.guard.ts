import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { CompanyCapabilitiesService } from './company-capabilities.service';

export const moduleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const capabilities = inject(CompanyCapabilitiesService);
  const router = inject(Router);
  const requiredSection = route.data['sectionCapability'] as string | undefined;
  if (!requiredSection) return true;
  return capabilities.load().pipe(
    map(() => capabilities.hasAdminSection(requiredSection)
      ? true
      : router.createUrlTree(['/admin/suscripcion'], { queryParams: { seccion: requiredSection } })),
    catchError(() => of(router.createUrlTree(['/admin/suscripcion'])))
  );
};
