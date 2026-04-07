import { inject } from '@angular/core';
import { ResolveFn, Router, UrlTree } from '@angular/router';
import { catchError, map, of, tap } from 'rxjs';
import { TenantContextService } from './tenant-context.service';
import { TenantSiteService } from './tenant-site.service';

export const tenantResolver: ResolveFn<true | UrlTree> = route => {
  const slug = route.paramMap.get('slug') ?? '';
  const router = inject(Router);
  const tenantContext = inject(TenantContextService);
  const tenantSiteService = inject(TenantSiteService);

  return tenantSiteService.getBySlug(slug).pipe(
    tap(config => tenantContext.setTenant(config)),
    map(() => true as const),
    catchError(() => {
      tenantContext.clearTenant();
      return of(router.createUrlTree(['/']));
    })
  );
};
