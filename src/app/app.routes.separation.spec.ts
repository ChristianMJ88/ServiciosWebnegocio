import { applicationRoutes } from './app.routes.application';
import { marketingRoutes } from './app.routes.marketing';

describe('separación de superficies web', () => {
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
