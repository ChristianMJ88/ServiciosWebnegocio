
import { Component, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { PlatformHostService } from '../../core/platform/platform-host.service';

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

  readonly heroHighlights = [
    'Minisitio web personalizable incluido.',
    'CRM, recepción y caja en una sola plataforma.',
    'Recordatorios por WhatsApp y correo para reducir ausencias.'
  ];

  readonly heroSignals = [
    {
      title: 'Te encuentran',
      body: 'Tu minisitio muestra servicios y recibe reservas.',
      icon: 'bi-window'
    },
    {
      title: 'Tu agenda se organiza',
      body: 'Cada cita llega con horario, servicio y responsable.',
      icon: 'bi-calendar2-check'
    },
    {
      title: 'Conoces a tus clientes',
      body: 'El CRM conserva historial, atención y seguimiento.',
      icon: 'bi-person-vcard'
    }
  ];

  readonly painPoints = [
    {
      title: 'Citas bajo control',
      body: 'Consulta horarios, servicios, responsables y estados sin depender de libretas, hojas de cálculo o mensajes sueltos.'
    },
    {
      title: 'Una imagen profesional',
      body: 'Publica un minisitio personalizable con información, imágenes, servicios, precios y formas de contacto.'
    },
    {
      title: 'Clientes que regresan',
      body: 'Conserva su historial y activa recordatorios y seguimientos para reducir ausencias y recuperar clientes.'
    }
  ];

  readonly pillars = [
    {
      title: 'Agenda y reservas',
      body: 'Registra citas, administra disponibilidad y permite que tus clientes reserven desde tu minisitio.'
    },
    {
      title: 'CRM de clientes',
      body: 'Mantén contactos, citas e historial organizados para brindar atención personalizada y dar seguimiento.'
    },
    {
      title: 'Operación completa',
      body: 'Coordina recepción, personal, caja, WhatsApp y correo institucional sin fragmentar la información.'
    }
  ];

  readonly workflow = [
    {
      step: '01',
      title: 'Publica',
      body: 'Personaliza tu minisitio con tu marca, información, servicios, precios e imágenes.'
    },
    {
      step: '02',
      title: 'Agenda',
      body: 'Tus clientes reservan y la cita aparece organizada para el equipo y recepción.'
    },
    {
      step: '03',
      title: 'Atiende y fideliza',
      body: 'Registra atención y cobro, envía recordatorios y da seguimiento para impulsar nuevas visitas.'
    }
  ];

  readonly capabilities = [
    {
      title: 'Menos ausencias',
      body: 'Envía confirmaciones y recordatorios de citas por WhatsApp y correo electrónico.'
    },
    {
      title: 'Información centralizada',
      body: 'Agenda, clientes, equipo, servicios y pagos comparten una sola fuente de información.'
    },
    {
      title: 'Más clientes recurrentes',
      body: 'Identifica a quién dar seguimiento y programa mensajes para invitarlo a regresar.'
    },
    {
      title: 'Costo razonable',
      body: 'Una solución pensada para emprendedoras, pequeños negocios y empresas medianas que quieren crecer.'
    }
  ];

  readonly featureGroups = [
    {
      title: 'Agenda y presencia digital',
      items: ['Agenda de citas', 'Reservas en línea', 'Minisitio personalizable', 'Servicios, precios e imágenes']
    },
    {
      title: 'Clientes y seguimiento',
      items: ['CRM de clientes', 'Historial de citas', 'Recordatorios automáticos', 'Campañas para volver a reservar']
    },
    {
      title: 'Operación del negocio',
      items: ['Recepción de clientes', 'Caja y cobros', 'Personal y disponibilidad', 'WhatsApp y correo institucional']
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
