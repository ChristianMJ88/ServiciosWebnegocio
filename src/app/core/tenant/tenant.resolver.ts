import { inject } from '@angular/core';
import { ResolveFn, Router, UrlTree } from '@angular/router';
import { catchError, map, of, tap } from 'rxjs';
import { TenantContextService } from './tenant-context.service';
import { TenantSiteService } from './tenant-site.service';
import { SeoService } from '../seo/seo.service';

export const tenantResolver: ResolveFn<true | UrlTree> = (route, state) => {
  const slug = route.paramMap.get('slug') ?? '';
  const router = inject(Router);
  const tenantContext = inject(TenantContextService);
  const tenantSiteService = inject(TenantSiteService);
  const seo = inject(SeoService);

  return tenantSiteService.getBySlug(slug).pipe(
    tap(config => {
      tenantContext.setTenant(config);
      seo.applyTenant(config, state.url);
    }),
    map(config => config.publicado ? true as const : router.createUrlTree(['/'])),
    catchError(() => {
      tenantContext.clearTenant();
      return of(router.createUrlTree(['/']));
    })
  );
};
