import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({
  selector: 'app-fluora-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './fluora-home.component.html',
  styleUrl: './fluora-home.component.css'
})
export class FluoraHomeComponent {
  private readonly router = inject(Router);
  readonly platformHost = inject(PlatformHostService);

  readonly heroHighlights = [
    'Centraliza WhatsApp, formularios web y redes en un solo flujo.',
    'Convierte conversaciones en oportunidades con CRM y automatización.',
    'Da seguimiento sin depender de tareas manuales.'
  ];

  readonly heroSignals = [
    'Atención',
    'Ventas',
    'Seguimiento'
  ];

  readonly painPoints = [
    {
      title: 'Mensajes dispersos',
      body: 'El equipo responde desde varios canales sin contexto unificado ni visibilidad completa del historial.'
    },
    {
      title: 'Seguimiento inconsistente',
      body: 'Los leads se enfrían porque el siguiente paso depende de memoria, notas o mensajes sueltos.'
    },
    {
      title: 'Poca claridad comercial',
      body: 'Hay actividad, pero cuesta saber qué oportunidades avanzan, quién responde y qué canal convierte mejor.'
    }
  ];

  readonly pillars = [
    {
      title: 'Inbox omnicanal',
      body: 'Reúne conversaciones de WhatsApp, web y redes en una sola operación para responder con orden.'
    },
    {
      title: 'CRM conectado',
      body: 'Cada conversación puede crear o actualizar una oportunidad con etapa, responsable e historial.'
    },
    {
      title: 'Automatización con IA',
      body: 'Clasifica, responde, asigna y activa seguimiento automático sin construir procesos complejos.'
    }
  ];

  readonly workflow = [
    {
      step: '01',
      title: 'Captura',
      body: 'Los mensajes entran desde distintos canales y se centralizan en una sola vista.'
    },
    {
      step: '02',
      title: 'Organiza',
      body: 'Refluora clasifica la intención, registra el lead y lo mueve al flujo comercial correcto.'
    },
    {
      step: '03',
      title: 'Convierte',
      body: 'Automatiza seguimiento, reduce tiempos de respuesta y da continuidad hasta el cierre.'
    }
  ];

  readonly capabilities = [
    {
      title: 'Atención más rápida',
      body: 'Responde antes sin ampliar equipo y sin depender de una sola persona.'
    },
    {
      title: 'Pipeline visible',
      body: 'Entiende qué conversaciones son oportunidades y en qué etapa se encuentra cada una.'
    },
    {
      title: 'Seguimiento real',
      body: 'Activa recordatorios y automatizaciones para que los leads no se queden a medias.'
    },
    {
      title: 'Operación simple',
      body: 'Menos herramientas sueltas y más claridad para ventas, atención y seguimiento.'
    }
  ];

  readonly featureGroups = [
    {
      title: 'Conversaciones en un solo lugar',
      items: ['WhatsApp', 'Formularios web', 'Instagram y redes', 'Historial por contacto']
    },
    {
      title: 'CRM y automatización',
      items: ['Leads con etapa y responsable', 'Asignación automática', 'Seguimientos programados', 'Reglas por intención']
    },
    {
      title: 'Visibilidad y control',
      items: ['Estado del pipeline', 'Métricas por canal', 'Tiempos de respuesta', 'Trazabilidad del equipo']
    }
  ];

  readonly faqs = [
    {
      question: '¿Refluora es solo para WhatsApp?',
      answer: 'No. WhatsApp es una pieza importante, pero la plataforma está pensada para centralizar web, redes y otras entradas comerciales en un solo flujo.'
    },
    {
      question: '¿Es un CRM o una herramienta de automatización?',
      answer: 'Es una plataforma que combina conversaciones, CRM, automatización e inteligencia artificial para que el proceso comercial viva en un mismo sistema.'
    },
    {
      question: '¿Sirve para equipos pequeños?',
      answer: 'Sí. De hecho, uno de sus mayores beneficios es ayudar a equipos pequeños o medianos a responder mejor y seguir oportunidades con más consistencia.'
    },
    {
      question: '¿Puedo empezar simple y luego crecer?',
      answer: 'Sí. La idea es comenzar con una operación clara y después sumar más automatización, más canales y más estructura comercial según el crecimiento del negocio.'
    }
  ];

  constructor() {
    if (this.platformHost.hasDedicatedAppHost() && this.platformHost.isAppHost()) {
      void this.router.navigateByUrl('/acceso');
    }
  }
}
