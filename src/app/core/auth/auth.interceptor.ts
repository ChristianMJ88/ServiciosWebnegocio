import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getTokenAcceso();
  const esRutaAuth = req.url.includes('/auth/iniciar-sesion')
    || req.url.includes('/auth/registrar-cliente')
    || req.url.includes('/auth/refrescar-token')
    || req.url.includes('/auth/cerrar-sesion');
  const omitirRefresh = req.headers.has('X-Omitir-Refresh');

  if (!environment.apiBaseUrl || !req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  const requestConCredenciales = req.clone({ withCredentials: true });
  const requestConToken = token && !esRutaAuth
    ? requestConCredenciales.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
    : requestConCredenciales;

  return next(requestConToken).pipe(
    catchError(error => {
      if (error.status !== 401 || esRutaAuth || omitirRefresh || !authService.autenticado()) {
        return throwError(() => error);
      }

      return authService.refrescarToken().pipe(
        switchMap(nuevoToken => next(
          requestConCredenciales.clone({
            setHeaders: {
              Authorization: `Bearer ${nuevoToken}`
            }
          })
        )),
        catchError(refreshError => {
          authService.limpiarSesion();
          return throwError(() => refreshError);
        })
      );
    })
  );
};
