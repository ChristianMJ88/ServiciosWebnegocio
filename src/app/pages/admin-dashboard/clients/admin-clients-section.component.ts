import { AdminWorkspaceComponent } from '../shared/admin-workspace.component';
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs';
import { ClienteAdmin, ClientsService, GuardarCliente, FiltroClientesAdmin, MetadatosClientesAdmin } from '../../../core/admin/clients.service';

@Component({
  selector: 'app-admin-clients-section', standalone: true,
  imports: [AdminWorkspaceComponent,CommonModule, FormsModule, MatButtonModule, MatCardModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './admin-clients-section.component.html', styleUrl: './admin-clients-section.component.css'
})
export class AdminClientsSectionComponent implements OnInit {
  private readonly service = inject(ClientsService);
  readonly clientes = signal<ClienteAdmin[]>([]); readonly cargando = signal(false); readonly guardando = signal(false);
  readonly error = signal(''); readonly mensaje = signal(''); filtro = ''; editandoId: number | null = null;
  readonly pagina = signal(0); readonly totalPaginas = signal(0); readonly totalElementos = signal(0);
  readonly tamanoPagina = signal(0);
  readonly metadatos = signal<MetadatosClientesAdmin | null>(null);
  filtros: FiltroClientesAdmin = {};
  limpiarFiltros(): void { this.filtro=''; this.filtros={}; this.buscar(); }
  inicioPagina(): number { return this.clientes().length ? this.pagina() * this.tamanoPagina() + 1 : 0; }
  finPagina(): number { return this.clientes().length ? this.inicioPagina() + this.clientes().length - 1 : 0; }
  formulario: GuardarCliente = this.vacio();
  ngOnInit(): void { this.service.metadatos().subscribe({next: datos => this.metadatos.set(datos),error:()=>this.error.set('No se pudieron cargar los filtros del directorio.')}); this.cargar(); }
  cargar(pagina = this.pagina()): void { if (this.cargando()) return; this.cargando.set(true); this.error.set(''); this.service.listar(this.filtro, pagina, undefined, this.filtros).pipe(finalize(() => this.cargando.set(false))).subscribe({ next: v => { this.clientes.set(v.contenido); this.pagina.set(v.pagina); this.tamanoPagina.set(v.tamano); this.totalPaginas.set(v.totalPaginas); this.totalElementos.set(v.totalElementos); }, error: e => this.error.set(e?.error?.message || e?.error?.detail || e?.error?.mensaje || 'No se pudieron cargar los clientes.') }); }
  buscar(): void { this.cargar(0); }
  paginaAnterior(): void { if (this.pagina() > 0) this.cargar(this.pagina() - 1); }
  paginaSiguiente(): void { if (this.pagina() + 1 < this.totalPaginas()) this.cargar(this.pagina() + 1); }
  editar(c: ClienteAdmin): void { this.editandoId = c.id; this.formulario = { nombreCompleto: c.nombreCompleto, correo: c.correo, telefono: c.telefono, aceptaWhatsapp: c.aceptaWhatsapp, notas: c.notas }; }
  cancelar(): void { this.editandoId = null; this.formulario = this.vacio(); }
  guardar(): void {
    if (this.guardando()) return; this.guardando.set(true); this.error.set(''); this.mensaje.set('');
    const peticion = this.editandoId ? this.service.actualizar(this.editandoId, this.formulario) : this.service.crear(this.formulario);
    peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({ next: () => { this.mensaje.set(this.editandoId ? 'Cliente actualizado.' : 'Cliente creado.'); this.cancelar(); this.cargar(); }, error: e => this.error.set(e?.error?.message || e?.error?.detail || e?.error?.mensaje || 'No se pudo guardar el cliente.') });
  }
  private vacio(): GuardarCliente { return { nombreCompleto: '', correo: '', telefono: '', aceptaWhatsapp: true, notas: null }; }
}
