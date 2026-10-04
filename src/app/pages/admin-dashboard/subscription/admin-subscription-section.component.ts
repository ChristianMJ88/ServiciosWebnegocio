import { CommonModule, DOCUMENT } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { PlanPublicoFluora, PublicPlansService } from '../../../core/platform/public-plans.service';
import { AdminSubscriptionService, EstadoSuscripcionEmpresa } from './admin-subscription.service';

@Component({
  selector:'app-admin-subscription-section', standalone:true, imports:[CommonModule],
  templateUrl:'./admin-subscription-section.component.html', styleUrl:'./admin-subscription-section.component.css'
})
export class AdminSubscriptionSectionComponent implements OnInit {
  private readonly planesApi=inject(PublicPlansService);
  private readonly suscripcionApi=inject(AdminSubscriptionService);
  private readonly documento=inject(DOCUMENT);
  private readonly route=inject(ActivatedRoute);
  readonly cargando=signal(true);
  readonly procesando=signal<string|null>(null);
  readonly planes=signal<PlanPublicoFluora[]>([]);
  readonly estado=signal<EstadoSuscripcionEmpresa|null>(null);
  readonly error=signal<string|null>(null);
  readonly mensaje=signal<string|null>(null);

  ngOnInit(): void {
    const resultado=this.route.snapshot.queryParamMap.get('stripe');
    if(resultado==='exito')this.mensaje.set('Stripe recibió la solicitud. Confirmaremos la suscripción en cuanto llegue la notificación segura.');
    if(resultado==='cancelado')this.mensaje.set('El proceso se canceló y no se realizó ningún cargo.');
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true); this.error.set(null);
    forkJoin({planes:this.planesApi.listar(),estado:this.suscripcionApi.estado()}).pipe(finalize(()=>this.cargando.set(false))).subscribe({
      next:datos=>{this.planes.set(datos.planes);this.estado.set(datos.estado);},
      error:e=>this.error.set(this.errorApi(e))
    });
  }

  contratar(plan:PlanPublicoFluora,ciclo:'MENSUAL'|'ANUAL'): void {
    const clave=`${plan.id}-${ciclo}`; this.procesando.set(clave); this.error.set(null);
    this.suscripcionApi.checkout(plan.id,ciclo).pipe(finalize(()=>this.procesando.set(null))).subscribe({
      next:respuesta=>this.redirigir(respuesta.url), error:e=>this.error.set(this.errorApi(e))
    });
  }

  administrar(): void {
    this.procesando.set('portal'); this.error.set(null);
    this.suscripcionApi.portal().pipe(finalize(()=>this.procesando.set(null))).subscribe({
      next:respuesta=>this.redirigir(respuesta.url), error:e=>this.error.set(this.errorApi(e))
    });
  }

  precio(valor:number,codigo:string): string { return new Intl.NumberFormat('es-MX',{style:'currency',currency:codigo}).format(valor); }
  private redirigir(url:string): void { if(!/^https:\/\//i.test(url)) { this.error.set('Stripe devolvió una dirección no válida.'); return; } this.documento.location.assign(url); }
  private errorApi(error:any): string { return error?.error?.mensaje||error?.error?.message||error?.error?.detail||'No fue posible completar la operación.'; }
}
