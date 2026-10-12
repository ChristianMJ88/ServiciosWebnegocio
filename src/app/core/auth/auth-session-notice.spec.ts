import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { AccessNoticeService } from './access-notice.service';
import { PlatformAuthService } from '../platform/platform-auth.service';
import { SubscriptionAccessService } from '../subscription/subscription-access.service';
import { environment } from '../../../environments/environment';

describe('Aviso de sesión controlado por backend', () => {
  const auth = { getTokenAcceso: vi.fn(() => 'token'), autenticado: vi.fn(() => true), limpiarSesion: vi.fn(), refrescarToken: vi.fn() };
  const router = { navigate: vi.fn(() => Promise.resolve(true)) };
  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({ providers: [AccessNoticeService,
      { provide: AuthService, useValue: auth }, { provide: Router, useValue: router },
      { provide: PlatformAuthService, useValue: { token: () => null } },
      { provide: SubscriptionAccessService, useValue: { refresh: vi.fn() } }
    ] });
  });
  it('muestra el mensaje del backend y no renueva una sesión revocada', async () => {
    const error = new HttpErrorResponse({ status: 401, error: { codigo: 'SESION_NO_DISPONIBLE', mensaje: 'Mensaje emitido por el backend' } });
    const resultado = TestBed.runInInjectionContext(() => authInterceptor(new HttpRequest('GET', `${environment.apiBaseUrl}/admin/resumen`), () => throwError(() => error)));
    await expect(firstValueFrom(resultado)).rejects.toBe(error);
    expect(TestBed.inject(AccessNoticeService).mensaje()).toBe('Mensaje emitido por el backend');
    expect(auth.refrescarToken).not.toHaveBeenCalled(); expect(auth.limpiarSesion).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/acceso']);
  });
  it('no confunde falta de permisos con una cuenta bloqueada', async () => {
    const error = new HttpErrorResponse({ status: 403, error: { codigo: 'ACCESO_DENEGADO', mensaje: 'Sin permiso' } });
    const resultado = TestBed.runInInjectionContext(() => authInterceptor(new HttpRequest('GET', `${environment.apiBaseUrl}/admin/resumen`), () => throwError(() => error)));
    await expect(firstValueFrom(resultado)).rejects.toBe(error);
    expect(TestBed.inject(AccessNoticeService).mensaje()).toBeNull();
    expect(auth.limpiarSesion).not.toHaveBeenCalled(); expect(router.navigate).not.toHaveBeenCalled();
  });
  it('conserva el aviso si el backend revoca el acceso después de renovar el token', async () => {
    auth.refrescarToken.mockReturnValue(of('nuevo-token'));
    const vencido = new HttpErrorResponse({ status: 401 });
    const bloqueado = new HttpErrorResponse({ status: 401, error: { codigo: 'SESION_NO_DISPONIBLE', mensaje: 'Acceso revocado por el backend' } });
    const siguiente = vi.fn().mockReturnValueOnce(throwError(() => vencido)).mockReturnValueOnce(throwError(() => bloqueado));
    const resultado = TestBed.runInInjectionContext(() => authInterceptor(new HttpRequest('GET', `${environment.apiBaseUrl}/admin/resumen`), siguiente));
    await expect(firstValueFrom(resultado)).rejects.toBe(bloqueado);
    expect(TestBed.inject(AccessNoticeService).mensaje()).toBe('Acceso revocado por el backend');
    expect(router.navigate).toHaveBeenCalledWith(['/acceso']);
  });
});
