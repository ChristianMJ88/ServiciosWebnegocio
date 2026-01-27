import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="py-5">
      <div class="container py-5">
        <div class="text-center mb-5">
          <h1 class="display-4 fw-bold text-primary-color">Nuestros Servicios</h1>
          <p class="lead text-muted">Explora nuestra amplia gama de tratamientos para manos y pies.</p>
        </div>

        <div class="row g-4">
          <div class="col-lg-6" *ngFor="let cat of categories">
            <div class="card h-100 shadow-sm border-0 rounded-4 overflow-hidden">
              <div class="row g-0">
                <div class="col-md-4">
                  <img [src]="cat.image" class="img-fluid h-100 object-fit-cover" [alt]="cat.name">
                </div>
                <div class="col-md-8">
                  <div class="card-body p-4">
                    <h2 class="h4 fw-bold mb-4 border-bottom pb-2">{{ cat.name }}</h2>
                    <ul class="list-unstyled">
                      <li *ngFor="let item of cat.items" class="d-flex justify-content-between align-items-center mb-3">
                        <span class="fw-medium text-dark">{{ item.name }}</span>
                        <span class="badge rounded-pill bg-soft-primary text-primary-color fs-6">{{ item.price }}</span>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .text-primary-color { color: #e91e63; }
    .bg-soft-primary { background-color: rgba(233, 30, 99, 0.1); }
    .object-fit-cover { object-fit: cover; }
    @media (max-width: 767px) {
      .img-fluid { height: 200px !important; width: 100%; }
    }
  `]
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
