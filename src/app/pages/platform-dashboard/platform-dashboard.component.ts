import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { DetalleTenantPlataforma, EstadoStripePlataforma, LimitePlataforma, ModuloPlataforma, PlanPayload, PlanPlataforma, PlatformAdminService, ProveedorPagoPlataforma, ReglaComisionPayload, ReglaComisionPlataforma, ResumenPlataforma, SimulacionComision, SuscripcionPayload, SuscripcionPlataforma, TenantPlataforma } from '../../core/platform/platform-admin.service';
import { PlatformAuthService } from '../../core/platform/platform-auth.service';

@Component({
  selector: 'app-platform-dashboard', standalone: true, imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './platform-dashboard.component.html', styleUrl: './platform-dashboard.component.css'
})
export class PlatformDashboardComponent implements OnInit {
  private readonly api = inject(PlatformAdminService);
  readonly auth = inject(PlatformAuthService);
  readonly seccion = inject(ActivatedRoute).snapshot.data['seccion'] as 'resumen'|'negocios'|'suscripciones'|'comisiones';
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly resumen = signal<ResumenPlataforma | null>(null);
  readonly tenants = signal<TenantPlataforma[]>([]);
  readonly menuAbierto = signal(false);
  readonly proveedores = signal<ProveedorPagoPlataforma[]>([]);
  readonly reglas = signal<ReglaComisionPlataforma[]>([]);
  readonly simulacion = signal<SimulacionComision | null>(null);
  readonly guardando = signal(false);
  readonly mensaje = signal<string | null>(null);
  readonly detalleTenant = signal<DetalleTenantPlataforma | null>(null);
  readonly cargandoDetalle = signal(false);
  readonly modulosSaas = signal<ModuloPlataforma[]>([]);
  readonly limitesSaas = signal<LimitePlataforma[]>([]);
  readonly planesSaas = signal<PlanPlataforma[]>([]);
  readonly suscripcionesSaas = signal<SuscripcionPlataforma[]>([]);
  readonly stripeEstado = signal<EstadoStripePlataforma | null>(null);
  readonly sincronizandoStripeId = signal<number | null>(null);
  planEditando: number | null = null;
  planFormulario: PlanPayload = this.planVacio();
  suscripcionFormulario: SuscripcionPayload = this.suscripcionVacia();
  reglaEditando: number | null = null;
  importeSimulacion = 1000;
  formulario: ReglaComisionPayload = this.formularioVacio();
  readonly busqueda = signal('');
  readonly filtrados = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    return termino ? this.tenants().filter(item => `${item.nombre} ${item.slug} ${item.estado}`.toLowerCase().includes(termino)) : this.tenants();
  });

  ngOnInit(): void { this.cargar(); }
  cargar(): void {
    this.cargando.set(true); this.error.set(null);
    if (this.seccion === 'comisiones') {
      forkJoin({ resumen: this.api.resumen(), tenants: this.api.tenants(), proveedores: this.api.proveedoresPago(), reglas: this.api.reglasComision() })
        .pipe(finalize(() => this.cargando.set(false))).subscribe({
          next: datos => { this.resumen.set(datos.resumen); this.tenants.set(datos.tenants); this.proveedores.set(datos.proveedores); this.reglas.set(datos.reglas); if (!this.formulario.proveedorId && datos.proveedores.length) this.formulario.proveedorId = datos.proveedores[0].id; },
          error: error => this.error.set(this.errorApi(error))
        });
      return;
    }
    if (this.seccion === 'suscripciones') {
      forkJoin({ resumen: this.api.resumen(), tenants: this.api.tenants(), modulos: this.api.modulosSuscripcion(), limites: this.api.limitesSuscripcion(), planes: this.api.planes(), suscripciones: this.api.suscripciones(), stripe: this.api.estadoStripe() })
        .pipe(finalize(() => this.cargando.set(false))).subscribe({ next: datos => {
          this.resumen.set(datos.resumen); this.tenants.set(datos.tenants); this.modulosSaas.set(datos.modulos); this.limitesSaas.set(datos.limites);
          this.planesSaas.set(datos.planes); this.suscripcionesSaas.set(datos.suscripciones); this.stripeEstado.set(datos.stripe);
        }, error: e => this.error.set(this.errorApi(e)) });
      return;
    }
    forkJoin({ resumen: this.api.resumen(), tenants: this.api.tenants() }).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: datos => { this.resumen.set(datos.resumen); this.tenants.set(datos.tenants); }, error: error => this.error.set(this.errorApi(error)) });
  }
  moneda(valor: number, codigo: string | null): string {
    if (!codigo) return valor === 0 ? '$0.00' : 'Varias monedas';
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: codigo }).format(valor);
  }
  simular(): void { this.error.set(null); this.api.simularComision(this.formulario, this.importeSimulacion).subscribe({ next: valor => this.simulacion.set(valor), error: e => this.error.set(this.errorApi(e)) }); }
  guardar(): void {
    if (!this.formulario.proveedorId) { this.error.set('Selecciona un proveedor de pago.'); return; }
    this.guardando.set(true); this.error.set(null); this.mensaje.set(null);
    const operacion = this.reglaEditando ? this.api.actualizarRegla(this.reglaEditando, this.formulario) : this.api.crearRegla(this.formulario);
    operacion.pipe(finalize(() => this.guardando.set(false))).subscribe({ next: () => { this.mensaje.set(this.reglaEditando ? 'Regla actualizada y auditada.' : 'Regla creada y auditada.'); this.cancelarEdicion(); this.recargarReglas(); }, error: e => this.error.set(this.errorApi(e)) });
  }
  editar(regla: ReglaComisionPlataforma): void { this.reglaEditando = regla.id; this.formulario = { proveedorId: regla.proveedorId, empresaId: regla.empresaId, tipo: regla.tipo, valor: regla.valor, importeMinimo: regla.importeMinimo, importeMaximo: regla.importeMaximo, moneda: regla.moneda, prioridad: regla.prioridad, vigenteDesde: this.fechaLocal(regla.vigenteDesde), vigenteHasta: regla.vigenteHasta ? this.fechaLocal(regla.vigenteHasta) : null, activa: regla.activa }; this.simulacion.set(null); window.scrollTo({ top: 0, behavior: 'smooth' }); }
  cambiarEstado(regla: ReglaComisionPlataforma): void { this.api.cambiarEstadoRegla(regla.id, !regla.activa).subscribe({ next: () => this.recargarReglas(), error: e => this.error.set(this.errorApi(e)) }); }
  abrirTenant(id: number): void { this.cargandoDetalle.set(true); this.detalleTenant.set(null); this.api.tenant(id).pipe(finalize(() => this.cargandoDetalle.set(false))).subscribe({ next: detalle => this.detalleTenant.set(detalle), error: e => this.error.set(this.errorApi(e)) }); }
  cerrarTenant(): void { this.detalleTenant.set(null); }
  moduloSeleccionado(id: number): boolean { return this.planFormulario.modulos.some(item => item.moduloId === id); }
  alternarModulo(id: number, seleccionado: boolean): void { this.planFormulario.modulos = seleccionado ? [...this.planFormulario.modulos,{moduloId:id,limiteUso:null}] : this.planFormulario.modulos.filter(item=>item.moduloId!==id); }
  valorLimite(id: number): number | null { return this.planFormulario.limites.find(item=>item.limiteId===id)?.valor ?? null; }
  cambiarLimite(id: number, valor: string): void { const numero=valor===''?null:Number(valor); this.planFormulario.limites=this.planFormulario.limites.filter(item=>item.limiteId!==id); if(numero!==null&&Number.isFinite(numero)&&numero>=0)this.planFormulario.limites.push({limiteId:id,valor:numero}); }
  guardarPlan(): void {
    if (!this.planFormulario.modulos.length) { this.error.set('Selecciona al menos un módulo para el plan.'); return; }
    this.guardando.set(true); this.error.set(null); this.mensaje.set(null);
    const operacion=this.planEditando?this.api.actualizarPlan(this.planEditando,this.planFormulario):this.api.crearPlan(this.planFormulario);
    operacion.pipe(finalize(()=>this.guardando.set(false))).subscribe({next:()=>{this.mensaje.set(this.planEditando?'Plan actualizado y auditado.':'Plan creado y auditado.');this.cancelarPlan();this.recargarSuscripciones();},error:e=>this.error.set(this.errorApi(e))});
  }
  editarPlan(plan: PlanPlataforma): void { this.planEditando=plan.id; this.planFormulario={codigo:plan.codigo,nombre:plan.nombre,descripcion:plan.descripcion,moneda:plan.moneda,precioMensual:plan.precioMensual,precioAnual:plan.precioAnual,activo:plan.activo,publico:plan.publico,destacado:plan.destacado,diasPrueba:plan.diasPrueba,diasGracia:plan.diasGracia,vigenteDesde:plan.vigenteDesde?this.fechaLocal(plan.vigenteDesde):null,vigenteHasta:plan.vigenteHasta?this.fechaLocal(plan.vigenteHasta):null,modulos:plan.modulos.map(m=>({moduloId:m.moduloId,limiteUso:m.limiteUso})),limites:plan.limites.map(l=>({limiteId:l.limiteId,valor:l.valor}))}; window.scrollTo({top:0,behavior:'smooth'}); }
  cancelarPlan(): void { this.planEditando=null; this.planFormulario=this.planVacio(); }
  asignarSuscripcion(): void { if(!this.suscripcionFormulario.empresaId||!this.suscripcionFormulario.planId){this.error.set('Selecciona negocio y plan.');return;} this.guardando.set(true);this.error.set(null);this.api.asignarSuscripcion(this.suscripcionFormulario).pipe(finalize(()=>this.guardando.set(false))).subscribe({next:()=>{this.mensaje.set('Suscripción asignada y auditada.');this.suscripcionFormulario=this.suscripcionVacia();this.recargarSuscripciones();},error:e=>this.error.set(this.errorApi(e))}); }
  sincronizarPlanStripe(plan: PlanPlataforma): void {
    this.sincronizandoStripeId.set(plan.id); this.error.set(null); this.mensaje.set(null);
    this.api.sincronizarPlanStripe(plan.id).pipe(finalize(()=>this.sincronizandoStripeId.set(null))).subscribe({
      next: resultado => this.mensaje.set(resultado.actualizado ? `${plan.nombre} quedó sincronizado con Stripe.` : `${plan.nombre} ya estaba actualizado en Stripe.`),
      error: e => this.error.set(this.errorApi(e))
    });
  }
  private recargarSuscripciones(): void { forkJoin({planes:this.api.planes(),suscripciones:this.api.suscripciones()}).subscribe(datos=>{this.planesSaas.set(datos.planes);this.suscripcionesSaas.set(datos.suscripciones);}); }
  cancelarEdicion(): void { this.reglaEditando = null; this.formulario = this.formularioVacio(); const primero = this.proveedores()[0]; if (primero) this.formulario.proveedorId = primero.id; this.simulacion.set(null); }
  private recargarReglas(): void { this.api.reglasComision().subscribe(datos => this.reglas.set(datos)); }
  private formularioVacio(): ReglaComisionPayload { const ahora = new Date(); ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset()); return { proveedorId: null, empresaId: null, tipo: 'PORCENTAJE', valor: 1, importeMinimo: null, importeMaximo: null, moneda: 'MXN', prioridad: 0, vigenteDesde: ahora.toISOString().slice(0,16), vigenteHasta: null, activa: false }; }
  private planVacio(): PlanPayload { return {codigo:'',nombre:'',descripcion:null,moneda:'MXN',precioMensual:0,precioAnual:null,activo:false,publico:false,destacado:false,diasPrueba:14,diasGracia:3,vigenteDesde:null,vigenteHasta:null,modulos:[],limites:[]}; }
  private suscripcionVacia(): SuscripcionPayload { const ahora=new Date();ahora.setMinutes(ahora.getMinutes()-ahora.getTimezoneOffset());return {empresaId:null,planId:null,ciclo:'MENSUAL',precioAcordado:null,moneda:'MXN',estado:'ACTIVA',iniciaEn:ahora.toISOString().slice(0,16),terminaEn:null,pruebaHasta:null}; }
  private fechaLocal(valor: string): string { return valor.slice(0,16); }
  private errorApi(error: any): string { return error?.error?.mensaje || error?.error?.message || error?.error?.detail || 'No fue posible completar la operación.'; }
}
