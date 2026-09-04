import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { ClienteAdmin, ClientsService, GuardarCliente } from '../../../core/admin/clients.service';

@Component({
  selector: 'app-admin-clients-section', standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatCardModule, MatCheckboxModule, MatFormFieldModule, MatInputModule],
  templateUrl: './admin-clients-section.component.html', styleUrl: './admin-clients-section.component.css'
})
export class AdminClientsSectionComponent implements OnInit {
  private readonly service = inject(ClientsService);
  readonly clientes = signal<ClienteAdmin[]>([]); readonly cargando = signal(false); readonly guardando = signal(false);
  readonly error = signal(''); readonly mensaje = signal(''); filtro = ''; editandoId: number | null = null;
  formulario: GuardarCliente = this.vacio();
  ngOnInit(): void { this.cargar(); }
  cargar(): void { this.cargando.set(true); this.error.set(''); this.service.listar(this.filtro).pipe(finalize(() => this.cargando.set(false))).subscribe({ next: v => this.clientes.set(v), error: e => this.error.set(e?.error?.message || 'No se pudieron cargar los clientes.') }); }
  editar(c: ClienteAdmin): void { this.editandoId = c.id; this.formulario = { nombreCompleto: c.nombreCompleto, correo: c.correo, telefono: c.telefono, aceptaWhatsapp: c.aceptaWhatsapp, notas: c.notas }; }
  cancelar(): void { this.editandoId = null; this.formulario = this.vacio(); }
  guardar(): void {
    if (this.guardando()) return; this.guardando.set(true); this.error.set(''); this.mensaje.set('');
    const peticion = this.editandoId ? this.service.actualizar(this.editandoId, this.formulario) : this.service.crear(this.formulario);
    peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({ next: () => { this.mensaje.set(this.editandoId ? 'Cliente actualizado.' : 'Cliente creado.'); this.cancelar(); this.cargar(); }, error: e => this.error.set(e?.error?.message || 'No se pudo guardar el cliente.') });
  }
  private vacio(): GuardarCliente { return { nombreCompleto: '', correo: '', telefono: '', aceptaWhatsapp: true, notas: null }; }
}
