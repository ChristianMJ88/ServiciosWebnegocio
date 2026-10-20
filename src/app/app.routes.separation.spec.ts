import { applicationRoutes } from './app.routes.application';
import { marketingRoutes } from './app.routes.marketing';
import { tenantResolver } from './core/tenant/tenant.resolver';
import { tenantHostResolver } from './core/tenant/tenant-host.resolver';
import { tenantPublicStylesResolver } from './core/styles/tenant-public-styles';

describe('separación de superficies web', () => {
  it('waits for public styles on every route that resolves a tenant', () => {
    const tenantRoutes = marketingRoutes.filter(route => Object.values(route.resolve || {}).some(resolver => resolver === tenantResolver || resolver === tenantHostResolver));
    expect(tenantRoutes.length).toBeGreaterThan(1);
    for (const route of tenantRoutes) expect(route.resolve?.['estilos']).toBe(tenantPublicStylesResolver);
  });
  it('mantiene los paneles privados fuera del manifiesto de marketing', () => {
    const paths = marketingRoutes.map((route) => route.path);

    expect(paths).toContain('e/:slug');
    expect(paths).toContain('admin');
    expect(marketingRoutes.find((route) => route.path === 'admin')?.data?.['destination']).toBe('app');
    expect(marketingRoutes.find((route) => route.path === 'admin')?.canActivate).toBeUndefined();
  });

  it('mantiene el contenido público fuera del manifiesto de la aplicación', () => {
    const tenantRoute = applicationRoutes.find((route) => route.path === 'e/:slug');

    expect(tenantRoute).toBeUndefined();
    expect(applicationRoutes.find((route) => route.path === 'admin/:seccion')?.canActivate).toBeDefined();
  });

  it('usa un layout de autenticación sin navegación pública', () => {
    expect(applicationRoutes.find((route) => route.path === 'acceso')?.data?.['layout']).toBe('auth');
    expect(applicationRoutes.find((route) => route.path === 'registro')?.data?.['layout']).toBe('auth');
  });
});
