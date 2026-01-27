import { Component } from '@angular/core';

@Component({
  selector: 'app-contact',
  standalone: true,
  template: `
    <section class="py-5">
      <div class="container py-5">
        <div class="row g-5">
          <div class="col-lg-6">
            <h1 class="display-4 fw-bold text-primary-color mb-4">Contáctanos</h1>
            <p class="lead text-muted mb-5">Estamos aquí para resolver tus dudas y ayudarte a lucir espectacular.</p>

            <div class="d-flex mb-4">
              <div class="flex-shrink-0 text-primary-color fs-3 me-3">📍</div>
              <div>
                <h3 class="h5 fw-bold mb-1">Nuestra Ubicación</h3>
                <p class="text-muted">Calle Principal #123, Ciudad Belleza, CP 54000</p>
              </div>
            </div>

            <div class="d-flex mb-4">
              <div class="flex-shrink-0 text-primary-color fs-3 me-3">📞</div>
              <div>
                <h3 class="h5 fw-bold mb-1">Teléfono / WhatsApp</h3>
                <p class="text-muted">+123 456 7890</p>
              </div>
            </div>

            <div class="d-flex mb-5">
              <div class="flex-shrink-0 text-primary-color fs-3 me-3">📧</div>
              <div>
                <h3 class="h5 fw-bold mb-1">Correo Electrónico</h3>
                <p class="text-muted">hola@nailartstudio.com</p>
              </div>
            </div>

            <div class="card bg-light border-0 rounded-4 p-4">
              <h3 class="h5 fw-bold mb-3">Síguenos en redes</h3>
              <div class="d-flex gap-3">
                <a href="#" class="btn btn-outline-dark rounded-circle">Ig</a>
                <a href="#" class="btn btn-outline-dark rounded-circle">Fb</a>
                <a href="#" class="btn btn-outline-dark rounded-circle">Tk</a>
              </div>
            </div>
          </div>

          <div class="col-lg-6">
            <div class="ratio ratio-16x9 h-100 rounded-4 shadow-sm overflow-hidden bg-light d-flex align-items-center justify-content-center">
              <div class="text-center p-5">
                <span class="fs-1">🗺️</span>
                <p class="mt-3 fw-bold">Ecuentranos en el mapa</p>

                <iframe src="https://www.google.com/maps/embed?pb=!1m17!1m12!1m3!1d2270.503921553064!2d-98.23603752533727!3d19.045331519278296!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m2!1m1!2zMTnCsDAyJzQzLjIiTiA5OMKwMTQnMDUuMSJX!5e1!3m2!1ses!2smx!4v1767065822511!5m2!1ses!2smx"
                        width="600" height="450" style="border:0;" allowfullscreen=""
                        loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .text-primary-color { color: #e91e63; }
    .btn-outline-dark:hover { background-color: #e91e63; border-color: #e91e63; }
  `]
})
export class ContactComponent {}
