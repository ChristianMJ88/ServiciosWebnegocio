import { Component, inject, signal, OnInit, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ScheduleRequest, ScheduleRequestsService } from './schedule-requests.service';
@Component({
 selector:'app-admin-schedule-requests',standalone:true,imports:[FormsModule],
 template:`
 <section class="requests" aria-label="Autorizaciones de horarios">
  <header><div><h3>Solicitudes de cambio</h3><p>Los horarios actuales se conservan hasta aprobar la propuesta.</p></div><button type="button" (click)="load()" [disabled]="busy()">Actualizar solicitudes</button></header>
  @if(error()){<p role="alert">{{error()}}</p>}
  @for(r of requests();track r.id){
   <article><strong>{{r.prestadorNombre}}</strong><p>{{r.propuesta.horario.horaInicio.slice(0,5)}}–{{r.propuesta.horario.horaFin.slice(0,5)}} · Intervalo {{r.propuesta.horario.intervaloMinutos}} min</p>
    <p>Días: @for(d of r.propuesta.diasSemana;track d){<span>{{dayLabel(d)}} </span>}</p>
    <p>Vigencia: {{r.propuesta.horario.vigenteDesde || 'Sin inicio'}} — {{r.propuesta.horario.vigenteHasta || 'Sin fin'}} · {{r.propuesta.reemplazar ? 'Reemplazar intervalos' : 'Agregar o editar intervalo'}}</p>
    <button type="button" (click)="selected=r;password=''">Revisar y autorizar</button>
   </article>
  }@empty{<p>No hay solicitudes pendientes.</p>}
  @if(selected;as r){
   <form (ngSubmit)="resolve(true)" class="approval"><h4>Autorizar a {{r.prestadorNombre}}</h4><p>Confirma con tu contraseña. El backend validará la propuesta antes de aplicarla.</p>
    <label>Tu contraseña<input type="password" name="approvalPassword" [(ngModel)]="password" autocomplete="current-password" required></label>
    <div><button type="button" (click)="selected=null;password=''" [disabled]="busy()">Cancelar</button><button type="button" (click)="resolve(false)" [disabled]="busy() || !password">Rechazar</button><button type="submit" [disabled]="busy() || !password">Aprobar cambio</button></div>
   </form>
  }
 </section>`,
 styles:`:host{display:block;margin-bottom:20px}.requests{background:#fff;border:1px solid #dce5ef;border-radius:18px;padding:20px}header{display:flex;justify-content:space-between;gap:16px;align-items:center}h3,h4{margin:0 0 8px}p{color:#52657e;line-height:1.5}article,.approval{border-top:1px solid #e2e8f0;padding:18px 0}button{min-height:44px;border:1px solid #c9d7e8;border-radius:10px;padding:10px 14px;background:#f4f7fc;color:#244f8c;cursor:pointer}button:disabled{opacity:.5}label{display:grid;gap:8px}input{font:inherit;font-size:16px;min-height:44px;border:1px solid #c9d7e8;border-radius:10px;padding:8px}.approval div{display:flex;gap:10px;flex-wrap:wrap;margin-top:16px}@media(max-width:600px){header{align-items:stretch;flex-direction:column}.requests{padding:16px}.approval div button{flex:1}}`
})
export class AdminScheduleRequestsComponent implements OnInit {
 @Output() resolved = new EventEmitter<void>();
 private api=inject(ScheduleRequestsService);
 requests=signal<ScheduleRequest[]>([]);busy=signal(false);error=signal('');selected:ScheduleRequest|null=null;password='';days=signal<{valor:number;etiqueta:string}[]>([]);
 ngOnInit(){this.api.metadata().subscribe({next:m=>this.days.set(m.diasSemana)});this.load();}
 dayLabel(day:number){return this.days().find(d=>d.valor===day)?.etiqueta ?? String(day);}
 load(){this.busy.set(true);this.api.pending().subscribe({next:r=>{this.requests.set(r);this.busy.set(false);},error:()=>{this.error.set('No se pudieron cargar las solicitudes.');this.busy.set(false);}});}
 resolve(aprobar:boolean){if(!this.selected)return;this.busy.set(true);this.error.set('');this.api.resolve(this.selected.id,aprobar,this.password).subscribe({next:()=>{this.password='';this.selected=null;this.resolved.emit();this.load();},error:e=>{this.password='';this.busy.set(false);this.error.set(e.error?.mensaje || 'No se pudo autorizar el cambio. Revisa tu contraseña y los horarios actuales.');}});}
}
