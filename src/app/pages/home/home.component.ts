import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  trabajos = [
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
