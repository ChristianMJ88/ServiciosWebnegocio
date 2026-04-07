import { inject } from '@angular/core';
import { ResolveFn, Router, UrlTree } from '@angular/router';
import { catchError, map, of, tap } from 'rxjs';
import { PlatformHostService } from '../platform/platform-host.service';
import { TenantContextService } from './tenant-context.service';
import { TenantSiteService } from './tenant-site.service';

export const tenantHostResolver: ResolveFn<true | UrlTree> = () => {
  const router = inject(Router);
  const platformHost = inject(PlatformHostService);
  const tenantContext = inject(TenantContextService);
  const tenantSiteService = inject(TenantSiteService);

  if (!platformHost.isCustomTenantHost()) {
    tenantContext.clearTenant();
    return of(router.createUrlTree(['/']));
  }

  return tenantSiteService.getByHostname(platformHost.currentHostname()).pipe(
    tap(config => tenantContext.setTenant(config)),
    map(() => true as const),
    catchError(() => {
      tenantContext.clearTenant();
      return of(router.createUrlTree(['/']));
    })
  );
};
