import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environments/environment';
interface Configuracion { zonaPrecioId: number | null; zonas: {id:number; nombre:string; activa:boolean}[]; }
@Component({selector:'app-branch-price-zone',standalone:true,imports:[FormsModule],template:`
  <section class="zone-panel"><h4>Zona de precio para Caja</h4><p>Se utiliza automáticamente al cotizar y cobrar productos en esta sucursal. Sin zona se utiliza el precio base.</p>
  @if(configuracion(); as config){<label>Zona predeterminada<select [ngModel]="zona" (ngModelChange)="zona=$event" [ngModelOptions]="{standalone:true}" [disabled]="guardando()"><option [ngValue]="null">Sin zona · precio base</option>@for(opcion of config.zonas;track opcion.id){<option [ngValue]="opcion.id" [disabled]="!opcion.activa">{{opcion.nombre}}{{opcion.activa?'':' · Inactiva'}}</option>}</select></label><button type="button" (click)="guardar()" [disabled]="guardando()">{{guardando()?'Guardando…':'Guardar zona de precio'}}</button>}
  <p role="status">{{mensaje()}}</p></section>`,styles:[`.zone-panel{padding:18px;border:1px solid #dce7f4;border-radius:14px;background:#f8fafc}.zone-panel p{line-height:1.6;color:#56667c}.zone-panel label{display:grid;gap:8px}.zone-panel select,.zone-panel button{min-height:44px;padding:10px;border:1px solid #ccd8e5;border-radius:10px;background:white;font:inherit}.zone-panel button{margin-top:12px;color:#1d4ed8}.zone-panel select{width:100%;font-size:16px}`]})
export class BranchPriceZoneComponent implements OnChanges {
  @Input({required:true}) sucursalId!:number;
  private http=inject(HttpClient);
  configuracion=signal<Configuracion|null>(null); guardando=signal(false); mensaje=signal(''); zona:number|null=null;
  private url(){return `${environment.apiBaseUrl}/admin/sucursales/${this.sucursalId}/zona-precio`;}
  ngOnChanges(){this.configuracion.set(null);this.mensaje.set('Cargando zonas…');this.http.get<Configuracion>(this.url()).subscribe({next:c=>{this.configuracion.set(c);this.zona=c.zonaPrecioId;this.mensaje.set('');},error:()=>this.mensaje.set('No fue posible cargar las zonas de precio.')});}
  guardar(){this.guardando.set(true);this.http.patch<Configuracion>(this.url(),{zonaPrecioId:this.zona}).subscribe({next:c=>{this.configuracion.set(c);this.guardando.set(false);this.mensaje.set('Zona guardada para esta sucursal.');},error:e=>{this.guardando.set(false);this.mensaje.set(e.error?.mensaje||e.error?.detail||'No fue posible guardar la zona.');}});}
}
