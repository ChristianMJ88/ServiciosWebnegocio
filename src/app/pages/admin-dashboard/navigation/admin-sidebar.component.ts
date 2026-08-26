import { Component, computed, effect, input, output, signal } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GrupoSidebarAdminView, SeccionAdmin } from '../admin-dashboard.config';

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [MatTooltipModule],
  templateUrl: './admin-sidebar.component.html',
  styleUrl: './admin-sidebar.component.css'
})
export class AdminSidebarComponent {
  readonly grupos = input.required<readonly GrupoSidebarAdminView[]>();
  readonly seccionActiva = input.required<SeccionAdmin>();
  readonly compacto = input(false);
  readonly movil = input(false);
  readonly oculto = input(false);
  readonly inicialesEmpresa = input.required<string>();
  readonly nombreEmpresa = input.required<string>();
  readonly seleccionar = output<SeccionAdmin>();

  private readonly gruposExpandidos = signal<ReadonlySet<string>>(new Set());
  readonly mostrarContenido = computed(() => !this.compacto() || this.movil());

  constructor() {
    effect(() => {
      const grupoActivo = this.grupos().find(grupo => grupo.modulos.includes(this.seccionActiva()));
      if (grupoActivo && !this.gruposExpandidos().has(grupoActivo.id)) {
        this.gruposExpandidos.update(actuales => new Set([...actuales, grupoActivo.id]));
      }
    });
  }

  alternarGrupo(grupoId: string): void {
    this.gruposExpandidos.update(actuales => {
      const siguientes = new Set(actuales);
      siguientes.has(grupoId) ? siguientes.delete(grupoId) : siguientes.add(grupoId);
      return siguientes;
    });
  }

  grupoExpandido(grupoId: string): boolean {
    return this.gruposExpandidos().has(grupoId);
  }

  grupoActivo(grupo: GrupoSidebarAdminView): boolean {
    return grupo.modulos.includes(this.seccionActiva());
  }
}
