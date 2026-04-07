import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TenantContextService } from '../../core/tenant/tenant-context.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  readonly tenantContext = inject(TenantContextService);

  readonly estadisticas = [
    { valor: 'Agenda online', etiqueta: 'Reserva simple desde cualquier dispositivo' },
    { valor: 'WhatsApp', etiqueta: 'Atencion rapida si el cliente necesita ayuda' },
    { valor: 'Reserva clara', etiqueta: 'Servicios, contacto y accion principal visibles' }
  ];

  readonly heroHighlights = [
    {
      icono: 'bi bi-calendar2-check',
      titulo: 'Agenda simple',
      descripcion: 'Reserva en linea y confirma tu horario sin vueltas.'
    },
    {
      icono: 'bi bi-chat-dots',
      titulo: 'Contacto rapido',
      descripcion: 'WhatsApp y datos del negocio visibles desde el inicio.'
    },
    {
      icono: 'bi bi-stars',
      titulo: 'Experiencia clara',
      descripcion: 'La pagina se enfoca en decidir rapido y reservar mejor.'
    }
  ];

  readonly especialidades = [
    {
      etiqueta: 'Servicio destacado',
      titulo: 'Servicio principal',
      descripcion: 'Una opcion clara para quienes quieren reservar rapido y entender el valor del negocio desde la primera vista.',
      imagen: 'https://images.unsplash.com/photo-1604654894610-df63bc536371?ixlib=rb-1.2.1&auto=format&fit=crop&w=900&q=80',
      icono: 'bi-gem',
      detalles: ['Informacion clara', 'Reserva directa', 'Presentacion profesional']
    },
    {
      etiqueta: 'Personalizado',
      titulo: 'Atencion con estilo propio',
      descripcion: 'Servicios pensados para adaptarse al cliente, explicar el resultado esperado y facilitar la eleccion.',
      imagen: 'https://images.unsplash.com/photo-1632345031435-8727f6897d53?ixlib=rb-1.2.1&auto=format&fit=crop&w=900&q=80',
      icono: 'bi-palette2',
      detalles: ['Acompañamiento', 'Propuesta visual', 'Reserva agil']
    },
    {
      etiqueta: 'Bienestar',
      titulo: 'Experiencia cuidada',
      descripcion: 'El sitio comunica lo esencial del servicio y ayuda a generar confianza antes de la cita.',
      imagen: 'https://images.unsplash.com/photo-1522337660859-02fbefca4702?ixlib=rb-1.2.1&auto=format&fit=crop&w=900&q=80',
      icono: 'bi-flower1',
      detalles: ['Contacto visible', 'Decision rapida', 'Mejor conversion']
    }
  ];

  readonly testimonios = [
    {
      nombre: 'Maria G.',
      resumen: 'Reserva facil',
      comentario: 'La pagina me dejo claro que ofrecen y pude reservar sin perder tiempo buscando informacion.'
    },
    {
      nombre: 'Lucia R.',
      resumen: 'Atencion clara',
      comentario: 'Se siente mas directa y ordenada. En pocos segundos supe como contactarlas y agendar.'
    },
    {
      nombre: 'Ana S.',
      resumen: 'Mas confianza',
      comentario: 'Ver servicios, contacto y reseñas en una sola vista me dio mas seguridad para reservar.'
    }
  ];
}
