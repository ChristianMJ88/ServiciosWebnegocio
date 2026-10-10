import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MarketplaceService, DirectoryParticipation } from '../../../core/marketplace/marketplace.service';
@Component({selector:'app-site-marketplace',standalone:true,template:`<section aria-label="Directorio Fluora"><h3>Participación en el directorio</h3><p>Permite que nuevos clientes encuentren tu negocio en Fluora. Se muestran los datos públicos de tu sitio.</p>@if(state();as s){<p>{{s.participante?'Participación activada':'Participación desactivada'}}</p>@if(!s.disponible){<p>La publicación requiere una suscripción vigente que incluya Marketplace.</p>}<button type="button" [disabled]="saving()||(!s.participante&&!s.disponible)" (click)="save(!s.participante)">{{saving()?'Guardando…':s.participante?'Dejar de participar':'Participar en el directorio'}}</button>}@else if(!error()){<p>Cargando configuración…</p>}@if(error()){<p role="alert">{{error()}}</p>}</section>`,styles:[`section{padding:20px;border:1px solid #dce3ea;border-radius:16px;margin-top:20px}button{padding:12px 16px;border:0;border-radius:10px;background:var(--fluora-brand-action,#00f5a0);color:#172c27;font-weight:600}button:disabled{opacity:.5}`]})
export class SiteMarketplaceComponent {
 private readonly api=inject(MarketplaceService);
 private readonly destroy=inject(DestroyRef);
 readonly state=signal<DirectoryParticipation|null>(null);readonly saving=signal(false);readonly error=signal('');
 constructor(){this.api.participacion().pipe(takeUntilDestroyed(this.destroy)).subscribe({next:s=>this.state.set(s),error:()=>this.error.set('No se pudo cargar la participación.')});}
 save(value:boolean){this.saving.set(true);this.error.set('');this.api.guardar(value).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:s=>{this.state.set(s);this.saving.set(false);},error:()=>{this.saving.set(false);this.error.set('No se pudo guardar. Revisa tu acceso o inténtalo de nuevo.');}});}
}
