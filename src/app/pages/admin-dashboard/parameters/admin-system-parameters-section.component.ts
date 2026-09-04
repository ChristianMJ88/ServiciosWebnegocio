import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ParametroSistemaAdmin, SystemParametersService } from '../../../core/admin/system-parameters.service';

@Component({ selector: 'app-admin-system-parameters-section', standalone: true,
  imports: [FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './admin-system-parameters-section.component.html' })
export class AdminSystemParametersSectionComponent implements OnInit {
  private readonly service = inject(SystemParametersService); readonly parametros = signal<ParametroSistemaAdmin[]>([]);
  readonly error = signal(''); readonly guardando = signal<string | null>(null); valores: Record<string, string> = {};
  ngOnInit(): void { this.cargar(); }
  cargar(): void { this.service.listar().subscribe({next: ps => { this.parametros.set(ps); this.valores = Object.fromEntries(ps.map(p => [p.clave, p.valor])); }, error: e => this.error.set(e?.error?.message || 'No se pudieron cargar los parámetros.')}); }
  categorias(): string[] { return [...new Set(this.parametros().map(p => p.categoria))]; }
  guardar(p: ParametroSistemaAdmin): void { this.guardando.set(p.clave); this.service.guardar(p.clave, this.valores[p.clave]).subscribe({next: v => { this.reemplazar(v); this.guardando.set(null); }, error: e => { this.error.set(e?.error?.message || 'No se pudo guardar el parámetro.'); this.guardando.set(null); }}); }
  restablecer(p: ParametroSistemaAdmin): void { this.service.restablecer(p.clave).subscribe({next: v => { this.valores[v.clave] = v.valor; this.reemplazar(v); }, error: e => this.error.set(e?.error?.message || 'No se pudo restablecer el parámetro.')}); }
  private reemplazar(p: ParametroSistemaAdmin): void { this.parametros.update(lista => lista.map(actual => actual.clave === p.clave ? p : actual)); }
}
