import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <!-- Hero Section -->
    <header class="hero-section text-white d-flex align-items-center">
      <div class="container text-center">
        <h1 class="display-2 fw-bold mb-3 animate__animated animate__fadeInDown">Arte en tus Manos</h1>
        <p class="lead mb-4 fs-4 animate__animated animate__fadeInUp">Servicios profesionales de manicura, pedicura y diseños personalizados.</p>
        <div class="d-flex justify-content-center gap-3">
          <a routerLink="/agendar" class="btn btn-primary btn-lg px-5 py-3 rounded-pill shadow">Agendar Ahora</a>
          <a routerLink="/servicios" class="btn btn-light btn-lg px-5 py-3 rounded-pill shadow">Ver Servicios</a>
        </div>
      </div>
    </header>

    <!-- Características -->
    <section class="py-5 bg-light">
      <div class="container py-5">
        <div class="row g-4 text-center">
          <div class="col-md-4">
            <div class="card h-100 border-0 shadow-sm p-4 rounded-4">
              <div class="mb-3 text-primary fs-1">💅</div>
              <h3 class="fw-bold h4">Manicura y Gel</h3>
              <p class="text-muted">Técnicas modernas y duraderas para un acabado impecable.</p>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card h-100 border-0 shadow-sm p-4 rounded-4">
              <div class="mb-3 text-primary fs-1">🎨</div>
              <h3 class="fw-bold h4">Nail Art</h3>
              <p class="text-muted">Diseños únicos y personalizados que reflejan tu estilo.</p>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card h-100 border-0 shadow-sm p-4 rounded-4">
              <div class="mb-3 text-primary fs-1">🌿</div>
              <h3 class="fw-bold h4">Tratamientos Spa</h3>
              <p class="text-muted">Relájate con nuestras exfoliaciones y masajes hidratantes.</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Galería Rápida (Nuestros Trabajos) -->
    <section class="py-5">
      <div class="container text-center">
        <h2 class="display-5 fw-bold mb-5">Nuestros Trabajos</h2>
        <div class="row g-3">
          <div class="col-6 col-md-3">
            <img src="https://images.unsplash.com/photo-1604654894610-df63bc536371?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" alt="Nail Art 1" class="img-fluid rounded-3 shadow-sm hover-zoom">
          </div>
          <div class="col-6 col-md-3">
            <img src="https://images.unsplash.com/photo-1632345031435-8727f6897d53?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" alt="Nail Art 2" class="img-fluid rounded-3 shadow-sm hover-zoom">
          </div>
          <div class="col-6 col-md-3">
            <img src="https://images.unsplash.com/photo-1607922546583-2dc36c2375fb?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" alt="Nail Art 3" class="img-fluid rounded-3 shadow-sm hover-zoom">
          </div>
          <div class="col-6 col-md-3">
            <img src="https://images.unsplash.com/photo-1522337660859-02fbefca4702?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" alt="Nail Art 4" class="img-fluid rounded-3 shadow-sm hover-zoom">
          </div>
          <div class="col-6 col-md-3">
            <img src="https://images.unsplash.com/photo-1600050218417-0345ab7f19f0?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" alt="Nail Art 5" class="img-fluid rounded-3 shadow-sm hover-zoom">
          </div>
          <div class="col-6 col-md-3">
            <img src="https://images.unsplash.com/photo-1516738901171-8eb4fc13bd20?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" alt="Nail Art 6" class="img-fluid rounded-3 shadow-sm hover-zoom">
          </div>
          <div class="col-6 col-md-3">
            <img src="https://images.unsplash.com/photo-1510172951991-856a654063f9?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" alt="Nail Art 7" class="img-fluid rounded-3 shadow-sm hover-zoom">
          </div>
          <div class="col-6 col-md-3">
            <img src="https://images.unsplash.com/photo-1526045431048-f857369baa09?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" alt="Nail Art 8" class="img-fluid rounded-3 shadow-sm hover-zoom">
          </div>
        </div>
      </div>
    </section>

    <!-- Acerca de Nosotros & Historia -->
    <section id="nosotros" class="py-5 bg-light">
      <div class="container py-5">
        <div class="row align-items-center g-5">
          <div class="col-lg-6">
            <img src="https://images.unsplash.com/photo-1560750588-73207b1ef5b8?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80" alt="Nuestro Salón" class="img-fluid rounded-4 shadow-lg">
          </div>
          <div class="col-lg-6">
            <h2 class="display-5 fw-bold mb-4">Acerca de Nosotros</h2>
            <p class="lead text-muted mb-4">Nuestra pasión es resaltar tu belleza natural a través del arte en tus manos y pies.</p>
            <h3 class="h4 fw-bold mb-3">Nuestra Historia</h3>
            <p class="mb-4 text-secondary">
              NailArt Studio nació de un pequeño sueño en una habitación compartida, donde nuestra fundadora comenzó a experimentar con diseños únicos para sus amigas. La idea era simple: convertir cada uña en un lienzo de expresión personal.
            </p>
            <p class="mb-4 text-secondary">
              Con el paso de los años, lo que empezó como un pasatiempo se convirtió en un referente de elegancia y cuidado. Hoy, contamos con un equipo de profesionales apasionados y nos dirigimos a convertirnos en el centro de bienestar integral líder de la ciudad, siempre innovando con las últimas tendencias globales.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- ¿Por qué elegirnos? -->
    <section class="py-5">
      <div class="container">
        <div class="text-center mb-5">
          <h2 class="display-5 fw-bold">¿Por qué elegirnos?</h2>
          <p class="text-muted">Nos diferenciamos por nuestro compromiso con la excelencia</p>
        </div>
        <div class="row g-4 text-center">
          <div class="col-md-4">
            <div class="p-4 rounded-4 border-0 shadow-sm bg-white h-100">
              <div class="fs-1 mb-3">✨</div>
              <h4 class="fw-bold">Calidad Premium</h4>
              <p class="text-secondary">Utilizamos solo los mejores productos del mercado para asegurar la salud y durabilidad de tus uñas.</p>
            </div>
          </div>
          <div class="col-md-4">
            <div class="p-4 rounded-4 border-0 shadow-sm bg-white h-100">
              <div class="fs-1 mb-3">🛡️</div>
              <h4 class="fw-bold">Higiene Estricta</h4>
              <p class="text-secondary">Cumplimos con los más altos estándares de esterilización para tu seguridad y tranquilidad.</p>
            </div>
          </div>
          <div class="col-md-4">
            <div class="p-4 rounded-4 border-0 shadow-sm bg-white h-100">
              <div class="fs-1 mb-3">👩‍🎨</div>
              <h4 class="fw-bold">Artistas Expertos</h4>
              <p class="text-secondary">Nuestro equipo está en constante formación para ofrecerte las últimas técnicas y diseños del mundo.</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Tips, Cuidados y Tendencias -->
    <section class="py-5 bg-dark text-white">
      <div class="container py-5">
        <h2 class="display-5 fw-bold text-center mb-5">Tips, Cuidados y Tendencias</h2>
        <div class="row g-4">
          <div class="col-md-4">
            <div class="card h-100 bg-secondary border-0 text-white rounded-4 overflow-hidden">
              <img src="https://images.unsplash.com/photo-1519014816548-bf5fe059798b?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" class="card-img-top" alt="Tip de Cuidado">
              <div class="card-body p-4">
                <h5 class="fw-bold">Cuidado Post-Cita</h5>
                <p class="small text-light">Aplica aceite de cutícula todas las noches para mantener tus uñas hidratadas y prolongar la duración del gel.</p>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card h-100 bg-secondary border-0 text-white rounded-4 overflow-hidden">
              <img src="https://images.unsplash.com/photo-1604902396830-aca29e19b067?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" class="card-img-top" alt="Tendencia 2024">
              <div class="card-body p-4">
                <h5 class="fw-bold">Tendencia: Minimalismo</h5>
                <p class="small text-light">Este año, los diseños "Clean Girl" y las uñas "Milky" son la sensación. ¡Menos es más!</p>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card h-100 bg-secondary border-0 text-white rounded-4 overflow-hidden">
              <img src="https://images.unsplash.com/photo-1629191122340-e2b4d90f230f?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60" class="card-img-top" alt="Recomendación">
              <div class="card-body p-4">
                <h5 class="fw-bold">Protege tus manos</h5>
                <p class="small text-light">Usa guantes al realizar tareas domésticas para evitar que los químicos dañen tus uñas acrílicas o de gel.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Reseñas de Clientes -->
    <section class="py-5 bg-light">
      <div class="container">
        <h2 class="display-5 fw-bold text-center mb-5">Lo que dicen nuestras clientes</h2>
        <div class="row g-4">
          <div class="col-md-4">
            <div class="card h-100 border-0 shadow-sm p-4 rounded-4">
              <div class="mb-3 text-warning">⭐⭐⭐⭐⭐</div>
              <p class="fst-italic mb-3">"Excelente servicio, mis uñas quedaron hermosas y el diseño personalizado superó mis expectativas. ¡Súper recomendadas!"</p>
              <div class="d-flex align-items-center">
                <div class="fw-bold">María G.</div>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card h-100 border-0 shadow-sm p-4 rounded-4">
              <div class="mb-3 text-warning">⭐⭐⭐⭐⭐</div>
              <p class="fst-italic mb-3">"La atención es maravillosa. El spa de pies es lo más relajante que he probado. Volveré sin duda cada mes."</p>
              <div class="d-flex align-items-center">
                <div class="fw-bold">Lucía R.</div>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card h-100 border-0 shadow-sm p-4 rounded-4">
              <div class="mb-3 text-warning">⭐⭐⭐⭐⭐</div>
              <p class="fst-italic mb-3">"Increíble calidad en el gel. Me duraron intactas por 3 semanas. La puntualidad en las citas es un plus enorme."</p>
              <div class="d-flex align-items-center">
                <div class="fw-bold">Ana S.</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .hero-section {
      height: 85vh;
      background: linear-gradient(rgba(0,0,0,0.6), rgba(0,0,0,0.6)), url('https://images.unsplash.com/photo-1519014816548-bf5fe059798b?ixlib=rb-1.2.1&auto=format&fit=crop&w=1500&q=80');
      background-size: cover;
      background-position: center;
    }
    .btn-primary {
      background-color: #e91e63;
      border-color: #e91e63;
    }
    .btn-primary:hover {
      background-color: #c2185b;
      border-color: #c2185b;
    }
    .hover-zoom {
      transition: transform 0.3s;
      cursor: pointer;
    }
    .hover-zoom:hover {
      transform: scale(1.05);
    }
    h1 {
      text-shadow: 2px 2px 8px rgba(0,0,0,0.4);
    }
  `]
})
export class HomeComponent {}
