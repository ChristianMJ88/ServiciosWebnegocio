import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { ConfiguracionEnviosAdmin, EnvioPedidoAdmin, OrigenEnvioAdmin, ShippingAdminService } from './shipping-admin.service';
import { AddressInput, AddressPlatformConfig, AddressValidationService } from '../../../services/address-validation.service';
import { GoogleAddressAutocompleteComponent } from '../../../shared/google-address-autocomplete/google-address-autocomplete.component';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';

@Component({
  selector: 'app-site-shipping',
  standalone: true,
  imports: [CommonModule, FormsModule, GoogleAddressAutocompleteComponent, MoneyDisplayPipe],
  templateUrl: './site-shipping.component.html',
  styleUrl: './site-shipping.component.css'
})
export class SiteShippingComponent implements OnInit {
  private readonly servicio = inject(ShippingAdminService);
  private readonly direcciones = inject(AddressValidationService);
  readonly configuracion = signal<ConfiguracionEnviosAdmin | null>(null);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly probandoConexion = signal(false);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly mostrarConexion = signal(false);
  readonly configuracionDirecciones = signal<AddressPlatformConfig | null>(null);
  readonly pedidos = signal<EnvioPedidoAdmin[]>([]);
  token = '';
  eliminarToken = false;

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    forkJoin({ envios: this.servicio.obtener(), direcciones: this.direcciones.configuration(), pedidos: this.servicio.listarPedidos() }).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: ({ envios: configuracion, direcciones, pedidos }) => {
        this.configuracion.set(configuracion);
        this.configuracionDirecciones.set(direcciones);
        this.pedidos.set(pedidos);
        this.mostrarConexion.set(!configuracion.credencialDisponible);
      },
      error: error => this.error.set(error?.error?.detail || 'No fue posible cargar la configuración de envíos.')
    });
  }

  alternarConexion(): void {
    this.mostrarConexion.update(valor => !valor);
  }

  probarConexion(): void {
    if (this.probandoConexion()) return;
    this.probandoConexion.set(true);
    this.mensaje.set(null);
    this.error.set(null);
    this.servicio.probarConexion().pipe(finalize(() => this.probandoConexion.set(false))).subscribe({
      next: resultado => this.mensaje.set(
        `${resultado.mensaje}. ${resultado.paqueteriasDisponibles} paqueterías disponibles para ${resultado.codigoPais} en ${resultado.ambiente === 'PRUEBA' ? 'pruebas' : 'producción'}.`
      ),
      error: error => this.error.set(error?.error?.mensaje || error?.error?.detail || error?.error?.message || 'No fue posible comprobar la conexión con Envia.com.')
    });
  }

  cambiarModoCredencial(modo: ConfiguracionEnviosAdmin['modoCredencial']): void {
    const configuracion = this.configuracion();
    if (!configuracion) return;
    configuracion.modoCredencial = modo;
    this.mostrarConexion.set(modo === 'CUENTA_PROPIA');
  }

  guardar(): void {
    const configuracion = this.configuracion();
    if (!configuracion || this.guardando()) return;
    const incompleto = configuracion.origenes.find(origen => origen.configurado && !this.origenCompleto(origen));
    if (incompleto) {
      this.error.set(`Completa el domicilio de origen de ${incompleto.sucursalNombre}.`);
      return;
    }
    this.guardando.set(true);
    this.mensaje.set(null);
    this.error.set(null);
    this.servicio.guardar(configuracion, this.token, this.eliminarToken).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: guardada => {
        this.configuracion.set(guardada);
        this.token = '';
        this.eliminarToken = false;
        this.mensaje.set('Configuración de envíos guardada correctamente.');
      },
      error: error => this.error.set(error?.error?.mensaje || error?.error?.detail || error?.error?.message || 'No fue posible guardar la configuración.')
    });
  }

  activarOrigen(origen: OrigenEnvioAdmin): void {
    origen.configurado = true;
    origen.activo = true;
    origen.codigoPais ||= this.configuracionDirecciones()?.regionPredeterminada ?? '';
  }

  aplicarDireccion(origen: OrigenEnvioAdmin, direccion: Partial<AddressInput> & { direccionFormateada?: string }): void {
    if (direccion.calle) origen.calle = direccion.calle;
    if (direccion.numeroExterior) origen.numeroExterior = direccion.numeroExterior;
    if (direccion.numeroInterior) origen.numeroInterior = direccion.numeroInterior;
    if (direccion.colonia) origen.colonia = direccion.colonia;
    if (direccion.municipio) origen.municipio = direccion.municipio;
    if (direccion.estado) origen.estado = direccion.estado;
    if (direccion.codigoPostal) origen.codigoPostal = direccion.codigoPostal;
    if (direccion.codigoPais) origen.codigoPais = direccion.codigoPais;
    origen.direccionFormateada = direccion.direccionFormateada ?? null;
    origen.direccionValidacionEstado = 'PENDIENTE';
  }

  origenCompleto(origen: OrigenEnvioAdmin): boolean {
    return [origen.nombreContacto, origen.telefono, origen.calle, origen.numeroExterior, origen.colonia,
      origen.municipio, origen.estado, origen.codigoPostal, origen.codigoPais].every(valor => Boolean(valor?.trim()));
  }

  etiquetaEstado(estado: string): string {
    const etiquetas: Record<string, string> = {
      BLOQUEADO_CONFIGURACION: 'Configuración pendiente',
      BLOQUEADO_CREDENCIAL: 'Credencial pendiente',
      BLOQUEADO_ORIGEN: 'Origen pendiente',
      BLOQUEADO_DIMENSIONES: 'Dimensiones pendientes',
      REQUIERE_COTIZACION: 'Requiere cotización',
      GUIA_GENERADA: 'Guía generada',
      EN_TRANSITO: 'En tránsito',
      ENTREGADO: 'Entregado'
    };
    return etiquetas[estado] ?? estado.replaceAll('_', ' ').toLowerCase();
  }

  estadoListo(estado: string): boolean {
    return ['GUIA_GENERADA', 'EN_TRANSITO', 'ENTREGADO'].includes(estado);
  }
}
