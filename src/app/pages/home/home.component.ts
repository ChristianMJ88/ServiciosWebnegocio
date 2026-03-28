import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  readonly estadisticas = [
    {valor: '10+', etiqueta: 'años de experiencia'},
    {valor: '1.2k', etiqueta: 'citas atendidas'},
    {valor: '98%', etiqueta: 'clientas recurrentes'}
  ];

  readonly heroHighlights = [
    {
      icono: 'bi bi-calendar2-check',
      titulo: 'Agenda simple',
      descripcion: 'Reserva en línea y confirma tu horario sin vueltas.'
    },
    {
      icono: 'bi bi-shield-check',
      titulo: 'Higiene cuidada',
      descripcion: 'Herramientas desinfectadas y estaciones siempre ordenadas.'
    },
    {
      icono: 'bi bi-palette2',
      titulo: 'Diseño guiado',
      descripcion: 'Te ayudamos a elegir forma, color y acabado según tu estilo.'
    }
  ];

  readonly heroStoryPoints = [
    'Diagnóstico rápido para definir largo, forma y estilo.',
    'Preparación técnica e higiene cuidadas en cada paso.',
    'Recomendaciones para que tu manicure dure bonita por más tiempo.'
  ];

  readonly especialidades = [
    {
      etiqueta: 'Servicio estrella',
      titulo: 'Manicura de alta duración',
      descripcion: 'Preparación técnica, nivelación y acabados que conservan brillo, forma y resistencia durante semanas.',
      imagen: 'https://images.unsplash.com/photo-1604654894610-df63bc536371?ixlib=rb-1.2.1&auto=format&fit=crop&w=900&q=80',
      icono: 'bi-gem',
      detalles: ['Diagnóstico inicial de la uña', 'Sellado profesional', 'Acabado limpio y uniforme']
    },
    {
      etiqueta: 'Diseño personalizado',
      titulo: 'Nail art con identidad',
      descripcion: 'Creamos propuestas alineadas a tu estilo, evento o temporada para que cada set se vea pensado de principio a fin.',
      imagen: 'https://images.unsplash.com/photo-1632345031435-8727f6897d53?ixlib=rb-1.2.1&auto=format&fit=crop&w=900&q=80',
      icono: 'bi-palette2',
      detalles: ['Moodboard de referencia', 'Composición cromática', 'Detalles pintados a mano']
    },
    {
      etiqueta: 'Bienestar integral',
      titulo: 'Spa para manos y pies',
      descripcion: 'Rituales de cuidado profundo con enfoque en hidratación, descanso y una sensación real de renovación.',
      imagen: 'https://images.unsplash.com/photo-1522337660859-02fbefca4702?ixlib=rb-1.2.1&auto=format&fit=crop&w=900&q=80',
      icono: 'bi-flower1',
      detalles: ['Exfoliación suave', 'Hidratación intensiva', 'Masaje relajante']
    }
  ];

  readonly beneficios = [
    {
      icono: 'bi-stars',
      titulo: 'Resultado premium',
      descripcion: 'Selección de productos profesionales, aplicación precisa y una entrega pulida que se nota desde la primera vista.'
    },
    {
      icono: 'bi-shield-check',
      titulo: 'Protocolos impecables',
      descripcion: 'Herramientas desinfectadas, estaciones ordenadas y procesos consistentes para trabajar con seguridad y confianza.'
    },
    {
      icono: 'bi-calendar2-check',
      titulo: 'Experiencia sin fricción',
      descripcion: 'Agenda clara, atención puntual y recomendaciones posteriores para que tu cita se sienta cuidada de extremo a extremo.'
    }
  ];

  readonly consejos = [
    {
      etiqueta: 'Rutina',
      titulo: 'Cuidado post-cita',
      descripcion: 'Aplica aceite de cutícula cada noche y crema ligera en manos para prolongar brillo, elasticidad y mejor crecimiento.',
      lectura: '2 min'
    },
    {
      etiqueta: 'Tendencia',
      titulo: 'Minimalismo con textura',
      descripcion: 'Tonos lechosos, acabados glossy y acentos metálicos suaves están marcando una línea elegante y muy versátil.',
      lectura: '3 min'
    },
    {
      etiqueta: 'Salud',
      titulo: 'Protección diaria',
      descripcion: 'Usa guantes al limpiar o manipular químicos para evitar desgaste prematuro en el producto y en la uña natural.',
      lectura: '4 min'
    }
  ];

  readonly testimonios = [
    {
      nombre: 'María G.',
      resumen: 'Diseño personalizado',
      comentario: 'La propuesta quedó elegante y muy bien ejecutada. Se nota muchísimo la atención al detalle y la asesoría durante la cita.'
    },
    {
      nombre: 'Lucía R.',
      resumen: 'Pedicura spa',
      comentario: 'La experiencia fue relajante de verdad. El servicio se siente ordenado, profesional y muy bien pensado de principio a fin.'
    },
    {
      nombre: 'Ana S.',
      resumen: 'Gel de larga duración',
      comentario: 'El acabado se mantuvo impecable varias semanas. Además, la puntualidad y la limpieza del espacio me dieron mucha confianza.'
    }
  ];

  readonly trabajos = [
    {
      titulo: 'Manicura Rusa',
      descripcion: 'Precisión y elegancia en cada detalle para una durabilidad excepcional.',
      imagen: 'https://images.unsplash.com/photo-1604654894610-df63bc536371?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
      categoria: 'Premium'
    },
    {
      titulo: 'Diseño Floral',
      descripcion: 'Arte pintado a mano que refleja la delicadeza de la naturaleza.',
      imagen: 'https://images.unsplash.com/photo-1632345031435-8727f6897d53?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
      categoria: 'Nail Art'
    },
    {
      titulo: 'Efecto Espejo',
      descripcion: 'Acabado cromado moderno para un look audaz y sofisticado.',
      imagen: 'https://images.unsplash.com/photo-1632345031435-8727f6897d53?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
      categoria: 'Tendencia'
    },
    {
      titulo: 'Pedicura Spa',
      descripcion: 'Relajación total y cuidado profundo para tus pies.',
      imagen: 'https://images.unsplash.com/photo-1522337660859-02fbefca4702?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
      categoria: 'Bienestar'
    },
    {
      titulo: 'Minimalismo Gold',
      descripcion: 'Toques dorados sobre bases naturales para un estilo atemporal.',
      imagen: 'https://images.unsplash.com/photo-1604654894610-df63bc536371?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
      categoria: 'Elegante'
    },
    {
      titulo: 'Encapsulado',
      descripcion: 'Diseños tridimensionales protegidos bajo capas de gel cristalino.',
      imagen: 'https://images.unsplash.com/photo-1516738901171-8eb4fc13bd20?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
      categoria: 'Técnica'
    },
    {
      titulo: 'Baby Boomer',
      descripcion: 'El degradado perfecto entre rosa y blanco para un look natural.',
      imagen: 'https://images.unsplash.com/photo-1510172951991-856a654063f9?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
      categoria: 'Clásico'
    },
    {
      titulo: 'Geometría Moderna',
      descripcion: 'Líneas limpias y formas abstractas para personalidades únicas.',
      imagen: 'https://images.unsplash.com/photo-1526045431048-f857369baa09?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
      categoria: 'Nail Art'
    }
  ];
}
