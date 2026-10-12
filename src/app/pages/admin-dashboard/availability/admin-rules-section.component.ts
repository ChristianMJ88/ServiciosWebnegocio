import { AdminWorkspaceComponent } from '../shared/admin-workspace.component';

import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { PrestadorAdmin, SucursalAdmin, GuardarReglaDisponibilidadPayload, OpcionNumeroDisponibilidadAdmin, OpcionTextoDisponibilidadAdmin, ReglaDisponibilidadAdmin } from '../../../core/admin/admin.service';
import { SujetoDisponibilidadOption } from './admin-availability.types';

@Component({
  selector: 'app-admin-rules-section',
  standalone: true,
  imports: [AdminWorkspaceComponent,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
],
  templateUrl: './admin-rules-section.component.html',
  styleUrls: ['./admin-rules-section.component.css']
})
export class AdminRulesSectionComponent implements OnChanges {
  reglaPorEliminar: ReglaDisponibilidadAdmin | null = null;
  @Output() deleteRule = new EventEmitter<ReglaDisponibilidadAdmin>();
  diasSeleccionados:number[]=[];
  reemplazar=false;
  @Input() prestadores: PrestadorAdmin[] = [];
  @Input() sucursales: SucursalAdmin[] = [];
  @Output() configureSubject = new EventEmitter<{tipoSujeto:string;sujetoId:number}>();
  grupos:{clave:string;nombre:string;tipoSujeto:string;sujetoId:number;reglas:ReglaDisponibilidadAdmin[]}[]=[];
  ngOnChanges(changes:SimpleChanges){
   if(changes['formularioRegla']){this.diasSeleccionados=this.formularioRegla?.diaSemana ? [this.formularioRegla.diaSemana] : [];this.reemplazar=false;}
   if(changes['reglasDisponibilidad'] || changes['prestadores'] || changes['sucursales']){
    const grupos=new Map<string,{clave:string;nombre:string;tipoSujeto:string;sujetoId:number;reglas:ReglaDisponibilidadAdmin[]}>();
    for(const p of this.prestadores) grupos.set('PRESTADOR:'+p.usuarioId,{clave:'PRESTADOR:'+p.usuarioId,nombre:p.nombreMostrar,tipoSujeto:'PRESTADOR',sujetoId:p.usuarioId,reglas:[]});
    for(const b of this.sucursales) grupos.set('SUCURSAL:'+b.id,{clave:'SUCURSAL:'+b.id,nombre:b.nombre,tipoSujeto:'SUCURSAL',sujetoId:b.id,reglas:[]});
    for(const regla of this.reglasDisponibilidad ?? []){
     const clave=regla.tipoSujeto+':'+regla.sujetoId;
     if(!grupos.has(clave))grupos.set(clave,{clave,nombre:regla.sujetoNombre,tipoSujeto:regla.tipoSujeto,sujetoId:regla.sujetoId,reglas:[]});
     grupos.get(clave)!.reglas.push(regla);
    }
    this.grupos=[...grupos.values()];
   }
  }
  seleccionarDia(dia:number){this.diasSeleccionados=this.diasSeleccionados.includes(dia)?this.diasSeleccionados.filter(d=>d!==dia):[...this.diasSeleccionados,dia];}
  seleccionarTodos(){this.diasSeleccionados=this.diasSemana.map(d=>d.valor);}
  reglasDelDia(dia:number){return this.reglasDisponibilidad.filter(r=>r.tipoSujeto===this.formularioRegla.tipoSujeto&&r.sujetoId===this.formularioRegla.sujetoId&&r.diaSemana===dia);}
  etiquetaTipo(valor:string){return this.tiposSujeto.find(tipo=>tipo.valor===valor)?.etiqueta ?? valor;}
  guardar(){this.save.emit({diasSemana:[...this.diasSeleccionados],reemplazar:this.reemplazar});}
  @Input({ required: true }) reglaEditandoId!: number | null;
  @Input({ required: true }) formularioRegla!: GuardarReglaDisponibilidadPayload;
  @Input({ required: true }) guardandoRegla!: boolean;
  @Input({ required: true }) sujetosRegla!: SujetoDisponibilidadOption[];
  @Input({ required: true }) tiposSujeto!: OpcionTextoDisponibilidadAdmin[];
  @Input({ required: true }) diasSemana!: OpcionNumeroDisponibilidadAdmin[];
  @Input({ required: true }) intervaloMinimoMinutos!: number;
  @Input({ required: true }) reglasDisponibilidad!: ReglaDisponibilidadAdmin[];
  @Input({ required: true }) diasSemanaTexto!: Record<number, string>;

  @Output() clearEdit = new EventEmitter<void>();
  @Output() save = new EventEmitter<{diasSemana:number[];reemplazar:boolean}>();
  @Output() typeChange = new EventEmitter<string>();
  @Output() editRule = new EventEmitter<ReglaDisponibilidadAdmin>();
}
