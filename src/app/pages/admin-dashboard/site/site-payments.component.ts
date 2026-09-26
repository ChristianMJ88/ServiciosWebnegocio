import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';
import { ConexionProveedorPago, PaymentsAdminService } from './payments-admin.service';

@Component({
  selector: 'app-site-payments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './site-payments.component.html',
  styleUrl: './site-payments.component.css'
})
export class SitePaymentsComponent implements OnInit {
  private readonly servicio = inject(PaymentsAdminService);
  private readonly route = inject(ActivatedRoute);

  readonly conexion = signal<ConexionProveedorPago | null>(null);
  readonly cargando = signal(true);
  readonly procesando = signal(false);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const resultado = this.route.snapshot.queryParamMap.get('mercadoPagoOAuth');
    if (resultado === 'ok') this.mensaje.set('La cuenta de Mercado Pago quedó conectada correctamente.');
    if (resultado === 'error') {
      this.error.set(this.route.snapshot.queryParamMap.get('mensaje') || 'No fue posible conectar Mercado Pago.');
    }
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.servicio.estado().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: conexion => this.conexion.set(conexion),
      error: err => this.error.set(this.detalleError(err, 'No fue posible consultar el estado de pagos.'))
    });
  }

  conectar(): void {
    if (this.procesando()) return;
    this.procesando.set(true);
    this.error.set(null);
    this.servicio.iniciar().pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: respuesta => window.location.assign(respuesta.urlAutorizacion),
      error: err => this.error.set(this.detalleError(err, 'Mercado Pago todavía no está configurado en Fluora.'))
    });
  }

  conectarPruebas(): void {
    if (this.procesando()) return;
    this.procesando.set(true);
    this.error.set(null);
    this.servicio.conectarPruebas().pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: conexion => {
        this.conexion.set(conexion);
        this.mensaje.set('Las credenciales de prueba quedaron activadas para este negocio.');
      },
      error: err => this.error.set(this.detalleError(err, 'Las credenciales de prueba todavía no están configuradas.'))
    });
  }

  desconectar(): void {
    if (this.procesando() || !window.confirm('¿Desconectar Mercado Pago de este negocio? Los cobros en línea dejarán de estar disponibles.')) return;
    this.procesando.set(true);
    this.error.set(null);
    this.servicio.desconectar().pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: conexion => {
        this.conexion.set(conexion);
        this.mensaje.set('Mercado Pago se desconectó de este negocio.');
      },
      error: err => this.error.set(this.detalleError(err, 'No fue posible desconectar Mercado Pago.'))
    });
  }

  private detalleError(error: any, predeterminado: string): string {
    return error?.error?.message || error?.error?.detail || predeterminado;
  }
}
