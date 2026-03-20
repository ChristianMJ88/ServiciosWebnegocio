import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './services.component.html',
  styleUrls: ['./services.component.css']
})
export class ServicesComponent {
  categories = [
    {
      name: 'Manicura y Pedicura',
      image: 'https://images.unsplash.com/photo-1519014816548-bf5fe059798b?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80',
      items: [
        { name: 'Manicura Tradicional', price: '$150' },
        { name: 'Manicura en Gel', price: '$100' },
        { name: 'Pedicura Tradicional', price: '$100' },
        { name: 'Pedicura en Gel', price: '$150' }
      ]
    },
    {
      name: 'Uñas Artificiales',
      image: 'https://images.unsplash.com/photo-1632345031435-8727f6897d53?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80',
      items: [
        { name: 'Uñas Acrílicas (Set)', price: '$180' },
        { name: 'Uñas de Gel (Set)', price: '$200' },
        { name: 'Tips', price: '$89' },
        { name: 'Mantenimiento', price: '$100' }
      ]
    },
    {
      name: 'Arte y Spa',
      image: 'https://images.unsplash.com/photo-1522337660859-02fbefca4702?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80',
      items: [
        { name: 'Nail Art Personalizado', price: 'desde $300' },
        { name: 'Encapsulados', price: 'desde $250' },
        { name: 'Spa (Exfoliación + Masaje)', price: '$400' },
        { name: 'Tratamiento Parafina', price: '$120' }
      ]
    },
    {
      name: 'Especiales',
      image: 'https://images.unsplash.com/photo-1604654894610-df63bc536371?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80',
      items: [
        { name: 'Retiro de Producto', price: '$60' },
        { name: 'Venta de Productos Cuidado', price: 'Varía' }
      ]
    }
  ];
}
