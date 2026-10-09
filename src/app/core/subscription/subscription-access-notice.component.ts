import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
import { DatePipe } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, timer } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { SubscriptionAccessService } from './subscription-access.service';

@Component({
  selector: 'app-subscription-access-notice', standalone: true, imports: [DatePipe, A11yModule],
  template: `
    @if (internal() && access.notice(); as notice) {
      @if (notice.bloqueado && !regularization()) {
        <div class="access-backdrop">
          <section cdkTrapFocus [cdkTrapFocusAutoCapture]="true" role="alertdialog" aria-modal="true" aria-labelledby="subscription-access-title" aria-describedby="subscription-access-message" tabindex="-1" class="access-card">
            <span class="access-icon" aria-hidden="true">!</span>
            <h2 id="subscription-access-title">{{ notice.titulo }}</h2>
            <p id="subscription-access-message">{{ notice.mensaje }}</p>
            <div class="access-actions">
              @if (notice.puedeRegularizar) { <button type="button" autofocus (click)="regularize()">Regularizar pago</button> }
              <button type="button" class="secondary" (click)="logout()">Cerrar sesión</button>
            </div>
          </section>
        </div>
      } @else if (notice.advertencia || notice.bloqueado) {
        <aside class="access-banner" role="status">
          <strong>{{ notice.titulo }}</strong><span>{{ notice.mensaje }}</span>
          @if (notice.graciaHasta) { <span>Hasta {{ notice.graciaHasta + 'Z' | date:'medium' }}</span> }
          @if (notice.puedeRegularizar && !regularization()) { <button type="button" (click)="regularize()">Revisar pago</button> }
        </aside>
      }
    }
  `,
  styles: [`
    :host{display:block}.access-backdrop{position:fixed;inset:0;z-index:20000;display:grid;place-items:center;padding:20px;background:rgb(15 23 42 / .65);backdrop-filter:blur(5px)}
    .access-card{width:min(100%,460px);background:#fff;border-radius:24px;padding:32px;color:#172033;box-shadow:0 24px 80px #0003;max-height:calc(100dvh - 40px);overflow:auto}
    .access-icon{display:grid;place-items:center;width:48px;height:48px;border-radius:16px;background:#fff2d5;color:#9a5800;font-size:26px;font-weight:700;margin-bottom:20px}
    h2{font-size:24px;margin:0 0 12px;line-height:1.25}p{line-height:1.65;color:#536078;margin:0 0 24px}.access-actions{display:grid;gap:10px}
    button{min-height:48px;padding:12px 20px;border:0;border-radius:12px;background:var(--tenant-primary,#2563eb);color:#fff;font-weight:600;cursor:pointer}.secondary{background:#f0f3f8;color:#334155}button:focus-visible{outline:3px solid #94a3b8;outline-offset:3px}
    .access-banner{position:relative;z-index:1100;display:flex;flex-wrap:wrap;align-items:center;gap:8px 16px;padding:14px 24px;background:#fff3d8;color:#714600;border-bottom:1px solid #ead6ad}.access-banner span{font-size:14px}.access-banner button{margin-left:auto}
    @media(max-width:600px){.access-card{padding:24px;border-radius:20px}.access-banner{padding:12px 16px}.access-banner button{width:100%;margin:0}.access-banner strong,.access-banner span{width:100%}}
  `]
})
export class SubscriptionAccessNoticeComponent {
  readonly access=inject(SubscriptionAccessService);
  private readonly auth=inject(AuthService);
  private readonly router=inject(Router);
  private readonly destroy=inject(DestroyRef);
  readonly internal=signal(false);
  readonly regularization=signal(false);
  constructor() {
    effect(() => { if (this.auth.autenticado()) this.updateRoute(); else this.access.clear(); });
    this.router.events.pipe(filter(event=>event instanceof NavigationEnd),takeUntilDestroyed(this.destroy)).subscribe(()=>this.updateRoute());
    timer(60000,60000).pipe(takeUntilDestroyed(this.destroy)).subscribe(()=>{if(this.internal()&&this.auth.autenticado())this.access.refresh();});
  }
  private updateRoute():void {
    const path=this.router.url.split('?')[0];
    this.internal.set(['/admin','/caja','/recepcion','/staff'].some(prefix=>path===prefix||path.startsWith(prefix+'/')));
    this.regularization.set(path==='/admin/suscripcion');
    if(this.internal()&&this.auth.autenticado())this.access.refresh();
  }
  regularize():void { void this.router.navigate(['/admin/suscripcion']); }
  logout():void { this.auth.logout();void this.router.navigate(['/acceso']); }
}
