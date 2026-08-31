import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OnboardingService } from '../../core/onboarding/onboarding.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';

@Component({selector:'app-verify-email',standalone:true,template:`<main class="container py-5 text-center"><h1>{{ estado() }}</h1><p>{{ detalle() }}</p>@if (confirmado()) {<a class="btn btn-primary" [href]="platformHost.appUrl('/acceso')">Iniciar sesión</a>}</main>`})
export class VerifyEmailComponent implements OnInit {
  private readonly route=inject(ActivatedRoute); private readonly onboarding=inject(OnboardingService); readonly platformHost=inject(PlatformHostService);
  readonly estado=signal('Confirmando tu correo…'); readonly detalle=signal('Espera un momento.'); readonly confirmado=signal(false);
  ngOnInit(){const token=this.route.snapshot.queryParamMap.get('token'); if(!token){this.estado.set('Enlace no válido');this.detalle.set('Solicita un nuevo enlace de confirmación.');return;} this.onboarding.confirmarCorreo(token).subscribe({next:r=>{this.estado.set('Correo confirmado');this.detalle.set(r.mensaje);this.confirmado.set(true);},error:e=>{this.estado.set('No pudimos confirmar el correo');this.detalle.set(e?.error?.message||'El enlace expiró o ya fue utilizado.');}});}
}
