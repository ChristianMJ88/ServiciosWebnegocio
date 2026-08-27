
import { Component, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { PlatformHostService } from '../../core/platform/platform-host.service';
import { SeoService } from '../../core/seo/seo.service';

@Component({
  selector: 'app-fluora-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './fluora-home.component.html',
  styleUrl: './fluora-home.component.css'
})
export class FluoraHomeComponent {
  private readonly router = inject(Router);
  readonly platformHost = inject(PlatformHostService);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly seo = inject(SeoService);

  readonly heroHighlights = [
    'Agenda por integrante y disponibilidad sin cruces.',
    'Minisitio personalizable con reservas en línea.',
    'CRM, recepción y caja conectados.'
  ];

  readonly heroSignals = [
    {
      title: 'Publica tu negocio',
      body: 'Muestra tu marca, servicios, precios y contacto.',
      icon: 'bi-window'
    },
    {
      title: 'Recibe reservas',
      body: 'Tus clientes eligen servicio y horario disponible.',
      icon: 'bi-calendar2-check'
    },
    {
      title: 'Coordina al equipo',
      body: 'Cada cita llega a la agenda del integrante correcto.',
      icon: 'bi-person-vcard'
    },
    {
      title: 'Opera con contexto',
      body: 'Recepción, caja y CRM comparten la información.',
      icon: 'bi-diagram-3'
    }
  ];

  readonly painPoints = [
    {
      title: 'Reservas entre mensajes y libretas',
      body: 'Centraliza las citas para consultar horarios, servicios, responsables y estados desde un mismo lugar.'
    },
    {
      title: 'Horarios que se cruzan',
      body: 'Define la disponibilidad y los servicios de cada integrante para coordinar al equipo sin duplicar horarios.'
    },
    {
      title: 'Clientes sin continuidad',
      body: 'Conserva datos de contacto e historial de citas para que tu equipo pueda continuar la atención.'
    },
    {
      title: 'Operación fragmentada',
      body: 'Conecta agenda, recepción, caja y CRM para evitar capturar la misma información varias veces.'
    }
  ];

  readonly pillars = [
    {
      title: 'Presencia digital',
      body: 'Publica un minisitio con tu marca, portada, servicios, precios, contacto y reservas en línea.'
    },
    {
      title: 'Agenda y equipo',
      body: 'Coordina sucursales, servicios, horarios y disponibilidad individual sin perder la visión completa.'
    },
    {
      title: 'Clientes y comunicación',
      body: 'Mantén el CRM y el historial conectados con correo, recordatorios y la integración opcional de WhatsApp.'
    },
    {
      title: 'Operación diaria',
      body: 'Recepción, caja, cobros, roles y permisos trabajan sobre la misma información.'
    }
  ];

  readonly workflow = [
    {
      step: '01',
      title: 'Publica tu negocio',
      body: 'Personaliza tu minisitio con tu marca, información, servicios, precios e imágenes.'
    },
    {
      step: '02',
      title: 'Fluora valida',
      body: 'Relaciona sucursal, servicio, disponibilidad y responsable antes de registrar la reserva.'
    },
    {
      step: '03',
      title: 'Atiende con contexto',
      body: 'La cita continúa en recepción y caja mientras el CRM conserva el historial del cliente.'
    }
  ];

  readonly capabilities = [
    {
      title: 'Si trabajas por tu cuenta',
      body: 'Publica tus servicios, recibe reservas y organiza clientes sin depender de mensajes y libretas.'
    },
    {
      title: 'Si tienes un equipo',
      body: 'Coordina agendas, servicios y horarios individuales sin provocar cruces entre integrantes.'
    },
    {
      title: 'Si tu operación está creciendo',
      body: 'Organiza sucursales, roles, recepción, caja y atención con una visión compartida.'
    }
  ];

  readonly featureGroups = [
    {
      title: 'Presencia y reservas',
      items: ['Minisitio por empresa', 'Marca, portada y contacto', 'Servicios, imágenes y precios', 'Reservas en línea']
    },
    {
      title: 'Agenda y equipo',
      items: ['Agenda por integrante', 'Disponibilidad y excepciones', 'Servicios asignados', 'Sucursales, roles y permisos']
    },
    {
      title: 'Clientes y operación',
      items: ['CRM e historial de citas', 'Recepción de clientes', 'Caja y cobros', 'Correo y WhatsApp opcional']
    }
  ];

  readonly plans = [
    {
      name: 'Emprende',
      audience: 'Para comenzar con una imagen profesional',
      price: '399',
      featured: false,
      features: ['1 sucursal', 'Hasta 2 usuarios', 'Agenda y reservas en línea', 'Minisitio web personalizable', 'CRM de clientes', 'Correo institucional y recordatorios']
    },
    {
      name: 'Negocio',
      audience: 'Para equipos que quieren operar y crecer',
      price: '699',
      featured: true,
      features: ['1 sucursal', 'Hasta 8 usuarios', 'Todo lo incluido en Emprende', 'Recepción y control de llegada', 'Caja, cobros y reportes', 'WhatsApp y automatizaciones', 'Roles y permisos para el equipo']
    },
    {
      name: 'Pro',
      audience: 'Para empresas con mayor operación',
      price: '1,199',
      featured: false,
      features: ['Hasta 3 sucursales', 'Hasta 20 usuarios', 'Todo lo incluido en Negocio', 'Operación y reportes por sucursal', 'Automatizaciones avanzadas', 'Seguimiento de clientes', 'Soporte prioritario']
    }
  ];

  readonly faqs = [
    {
      question: '¿Qué es Fluora Agenda?',
      answer: 'Es una agenda de citas en línea para negocios de servicios que incluye minisitio web, CRM de clientes, recepción, caja y automatizaciones.'
    },
    {
      question: '¿Puedo publicar mis servicios y precios?',
      answer: 'Sí. Tu minisitio es personalizable y permite mostrar información del negocio, imágenes, servicios, precios y datos de contacto.'
    },
    {
      question: '¿Sirve para emprendedoras y negocios pequeños?',
      answer: 'Sí. Fluora está diseñada para ser fácil de usar y tener un costo razonable para emprendimientos, pequeños negocios y empresas medianas.'
    },
    {
      question: '¿Cómo ayuda a reducir las citas perdidas?',
      answer: 'Fluora puede enviar confirmaciones y recordatorios por WhatsApp y correo, además de conservar el historial para dar seguimiento a tus clientes.'
    }
  ];

  constructor() {
    this.seo.applyMarketing();
    this.title.setTitle('Fluora Agenda | Agenda de citas y minisitio para tu negocio');
    this.meta.updateTag({ name: 'description', content: 'Agenda de citas para pequeños y medianos negocios. Incluye minisitio web personalizable, CRM, recepción, caja y recordatorios por WhatsApp y correo.' });
    this.meta.updateTag({ name: 'keywords', content: 'agenda de citas, agenda para negocios, agenda online, minisitio web, CRM para pequeños negocios, recordatorio de citas, Fluora Agenda' });
    this.meta.updateTag({ property: 'og:title', content: 'Fluora Agenda | Organiza citas y haz crecer tu negocio' });
    this.meta.updateTag({ property: 'og:description', content: 'Agenda, minisitio web, clientes, recepción, caja y recordatorios en una plataforma accesible.' });
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    if (this.platformHost.hasDedicatedAppHost() && this.platformHost.isAppHost()) {
      void this.router.navigateByUrl('/acceso');
    }
  }
}
