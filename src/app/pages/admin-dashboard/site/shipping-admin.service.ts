import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface OrigenEnvioAdmin {
  sucursalId: number;
  sucursalNombre: string;
  sucursalActiva: boolean;
  configurado: boolean;
  activo: boolean;
  nombreContacto: string;
  telefono: string;
  correo: string | null;
  calle: string;
  numeroExterior: string;
  numeroInterior: string | null;
  colonia: string;
  municipio: string;
  estado: string;
  codigoPostal: string;
  codigoPais: string;
  referencias: string | null;
  direccionFormateada: string | null;
  direccionValidacionEstado: string;
}

export interface ConfiguracionEnviosAdmin {
  proveedor: string;
  habilitado: boolean;
  ambiente: 'PRUEBA' | 'PRODUCCION';
  modoCredencial: 'CUENTA_PROPIA' | 'PLATAFORMA';
  tokenConfigurado: boolean;
  credencialDisponible: boolean;
  generarGuiaAutomaticamente: boolean;
  permitirSeleccionPaquetera: boolean;
  tipoRecargo: 'NINGUNO' | 'PORCENTAJE' | 'IMPORTE';
  valorRecargo: number;
  origenes: OrigenEnvioAdmin[];
  onboarding: {
    conexionAutomaticaDisponible: boolean;
    registroUrl: string;
    credencialesUrl: string;
    modosCredencial: Array<{
      codigo: 'CUENTA_PROPIA' | 'PLATAFORMA';
      nombre: string;
      descripcion: string;
      disponible: boolean;
    }>;
  };
}

export interface PruebaConexionEnvio {
  conectada: boolean;
  proveedor: string;
  ambiente: 'PRUEBA' | 'PRODUCCION';
  codigoPais: string;
  paqueteriasDisponibles: number;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class ShippingAdminService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/admin/envios/configuracion`;

  obtener(): Observable<ConfiguracionEnviosAdmin> {
    return this.http.get<ConfiguracionEnviosAdmin>(this.url);
  }

  guardar(configuracion: ConfiguracionEnviosAdmin, token: string, eliminarToken: boolean): Observable<ConfiguracionEnviosAdmin> {
    return this.http.put<ConfiguracionEnviosAdmin>(this.url, {
      habilitado: configuracion.habilitado,
      ambiente: configuracion.ambiente,
      modoCredencial: configuracion.modoCredencial,
      token: token.trim() || null,
      eliminarToken,
      generarGuiaAutomaticamente: configuracion.generarGuiaAutomaticamente,
      permitirSeleccionPaquetera: configuracion.permitirSeleccionPaquetera,
      tipoRecargo: configuracion.tipoRecargo,
      valorRecargo: configuracion.tipoRecargo === 'NINGUNO' ? 0 : Number(configuracion.valorRecargo || 0),
      origenes: configuracion.origenes.filter(origen => origen.configurado).map(({ sucursalNombre, sucursalActiva, configurado, direccionFormateada, direccionValidacionEstado, ...origen }) => origen)
    });
  }

  probarConexion(): Observable<PruebaConexionEnvio> {
    return this.http.post<PruebaConexionEnvio>(`${this.url}/probar-conexion`, {});
  }
}
