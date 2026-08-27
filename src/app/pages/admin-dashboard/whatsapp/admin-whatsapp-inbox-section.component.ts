import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EnviarMensajeWhatsappPayload, MensajeWhatsappAdmin } from '../../../core/admin/admin.service';
import { ConversacionWhatsappVm } from './admin-whatsapp.types';

@Component({
  selector: 'app-admin-whatsapp-inbox-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule
  ],
  templateUrl: './admin-whatsapp-inbox-section.component.html',
  styleUrls: ['./admin-whatsapp-inbox-section.component.css']
})
export class AdminWhatsappInboxSectionComponent implements OnChanges {
  @Input({ required: true }) mensajes!: MensajeWhatsappAdmin[];
  @Input({ required: true }) enviando!: boolean;
  @Input() respuestasRapidas: readonly string[] = [];

  @Output() refresh = new EventEmitter<void>();
  @Output() sendMessage = new EventEmitter<EnviarMensajeWhatsappPayload>();

  filtro = '';
  telefonoSeleccionado: string | null = null;
  borrador = '';
  conversacionAbiertaEnMovil = false;
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['mensajes']) {
      const conversaciones = this.conversaciones;
      if (!this.telefonoSeleccionado && conversaciones.length) {
        this.telefonoSeleccionado = conversaciones[0].telefono;
      }
      if (this.telefonoSeleccionado && !conversaciones.some(conversacion => conversacion.telefono === this.telefonoSeleccionado)) {
        this.telefonoSeleccionado = conversaciones[0]?.telefono ?? null;
      }
    }
  }

  get conversaciones(): ConversacionWhatsappVm[] {
    const grupos = new Map<string, MensajeWhatsappAdmin[]>();
    const filtro = this.normalizar(this.filtro);

    for (const mensaje of this.mensajes ?? []) {
      const telefono = mensaje.telefono || 'Sin teléfono';
      if (filtro && !this.normalizar(`${telefono} ${mensaje.cuerpo ?? ''} ${mensaje.estado ?? ''}`).includes(filtro)) {
        continue;
      }
      grupos.set(telefono, [...(grupos.get(telefono) ?? []), mensaje]);
    }

    return Array.from(grupos.entries())
      .map(([telefono, mensajes]) => {
        const ordenados = [...mensajes].sort((a, b) => new Date(a.creadoEn).getTime() - new Date(b.creadoEn).getTime());
        const ultimoMensaje = ordenados[ordenados.length - 1];
        return {
          telefono,
          iniciales: this.inicialesTelefono(telefono),
          ultimoMensaje,
          mensajes: ordenados,
          entrantes: ordenados.filter(mensaje => this.esEntrante(mensaje)).length,
          salientes: ordenados.filter(mensaje => !this.esEntrante(mensaje)).length,
          errores: ordenados.filter(mensaje => this.tieneError(mensaje)).length
        };
      })
      .sort((a, b) => new Date(b.ultimoMensaje.creadoEn).getTime() - new Date(a.ultimoMensaje.creadoEn).getTime());
  }

  get conversacionSeleccionada(): ConversacionWhatsappVm | null {
    const conversaciones = this.conversaciones;
    return conversaciones.find(conversacion => conversacion.telefono === this.telefonoSeleccionado) ?? conversaciones[0] ?? null;
  }

  get totalEntrantes(): number {
    return (this.mensajes ?? []).filter(mensaje => this.esEntrante(mensaje)).length;
  }

  get totalSalientes(): number {
    return (this.mensajes ?? []).filter(mensaje => !this.esEntrante(mensaje)).length;
  }

  get totalErrores(): number {
    return (this.mensajes ?? []).filter(mensaje => this.tieneError(mensaje)).length;
  }

  seleccionarConversacion(telefono: string): void {
    this.telefonoSeleccionado = telefono;
    this.conversacionAbiertaEnMovil = true;
  }

  volverAConversaciones(): void {
    this.conversacionAbiertaEnMovil = false;
  }

  enviar(): void {
    const conversacion = this.conversacionSeleccionada;
    const mensaje = this.borrador.trim();
    if (!conversacion || !mensaje || this.enviando) {
      return;
    }
    this.sendMessage.emit({ telefono: conversacion.telefono, mensaje });
    this.borrador = '';
  }

  usarRespuestaRapida(texto: string): void {
    this.borrador = texto;
  }

  esEntrante(mensaje: MensajeWhatsappAdmin): boolean {
    return (mensaje.direccion || '').toUpperCase() === 'ENTRANTE';
  }

  tieneError(mensaje: MensajeWhatsappAdmin): boolean {
    return !!mensaje.codigoErrorProveedor || !!mensaje.detalleErrorProveedor || (mensaje.estado || '').toUpperCase() === 'ERROR';
  }

  vistaPrevia(mensaje: MensajeWhatsappAdmin): string {
    if (mensaje.cuerpo?.trim()) {
      return mensaje.cuerpo.trim();
    }
    if (mensaje.contentSid) {
      return `Plantilla ${mensaje.contentSid}`;
    }
    return 'Mensaje sin texto';
  }

  estadoVisible(mensaje: MensajeWhatsappAdmin): string {
    const estado = mensaje.estado || (this.esEntrante(mensaje) ? 'Recibido' : 'Enviado');
    return estado.replaceAll('_', ' ').toLowerCase();
  }

  telefonoVisible(telefono: string): string {
    const digitos = telefono.replace(/\D/g, '');
    if (digitos.length <= 10) {
      return telefono;
    }
    return `+${digitos.slice(0, digitos.length - 10)} ${digitos.slice(-10, -7)} ${digitos.slice(-7, -4)} ${digitos.slice(-4)}`;
  }

  trackByTelefono(_: number, conversacion: ConversacionWhatsappVm): string {
    return conversacion.telefono;
  }

  trackByMensaje(_: number, mensaje: MensajeWhatsappAdmin): number {
    return mensaje.id;
  }

  private inicialesTelefono(telefono: string): string {
    const digitos = telefono.replace(/\D/g, '');
    return digitos.slice(-2) || 'WA';
  }

  private normalizar(valor: string): string {
    return valor.trim().toLowerCase().normalize('NFD').replace(/\p{M}+/gu, '');
  }
}
