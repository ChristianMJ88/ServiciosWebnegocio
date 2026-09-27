import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConfiguracionEnviosAdmin, OrigenEnvioAdmin, ShippingAdminService } from './shipping-admin.service';

@Component({
  selector: 'app-site-shipping',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './site-shipping.component.html',
  styleUrl: './site-shipping.component.css'
})
export class SiteShippingComponent implements OnInit {
  private readonly servicio = inject(ShippingAdminService);
  readonly configuracion = signal<ConfiguracionEnviosAdmin | null>(null);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly mostrarConexion = signal(false);
  token = '';
  eliminarToken = false;

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this.servicio.obtener().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: configuracion => {
        this.configuracion.set(configuracion);
        this.mostrarConexion.set(!configuracion.credencialDisponible);
      },
      error: error => this.error.set(error?.error?.detail || 'No fue posible cargar la configuración de envíos.')
    });
  }

  alternarConexion(): void {
    this.mostrarConexion.update(valor => !valor);
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
      error: error => this.error.set(error?.error?.detail || error?.error?.message || 'No fue posible guardar la configuración.')
    });
  }

  activarOrigen(origen: OrigenEnvioAdmin): void {
    origen.configurado = true;
    origen.activo = true;
    origen.codigoPais ||= 'MX';
  }

  origenCompleto(origen: OrigenEnvioAdmin): boolean {
    return [origen.nombreContacto, origen.telefono, origen.calle, origen.numeroExterior, origen.colonia,
      origen.municipio, origen.estado, origen.codigoPostal, origen.codigoPais].every(valor => Boolean(valor?.trim()));
  }
}
