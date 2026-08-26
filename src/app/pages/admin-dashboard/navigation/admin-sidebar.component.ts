import { Component, effect, input, OnDestroy, output, signal } from '@angular/core';
import { GrupoSidebarAdminView, SeccionAdmin } from '../admin-dashboard.config';

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  templateUrl: './admin-sidebar.component.html',
  styleUrl: './admin-sidebar.component.css'
})
export class AdminSidebarComponent implements OnDestroy {
  readonly grupos = input.required<readonly GrupoSidebarAdminView[]>();
  readonly seccionActiva = input.required<SeccionAdmin>();
  readonly compacto = input(false);
  readonly inicialesEmpresa = input.required<string>();
  readonly nombreEmpresa = input.required<string>();
  readonly seleccionar = output<SeccionAdmin>();

  private readonly gruposExpandidos = signal<ReadonlySet<string>>(new Set());
  readonly grupoFlyout = signal<GrupoSidebarAdminView | null>(null);
  readonly flyoutTop = signal(0);
  private cierreFlyoutId: ReturnType<typeof setTimeout> | null = null;

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

  ngOnDestroy(): void {
    this.cancelarCierreFlyout();
  }

  grupoExpandido(grupoId: string): boolean {
    return this.gruposExpandidos().has(grupoId);
  }

  grupoActivo(grupo: GrupoSidebarAdminView): boolean {
    return grupo.modulos.includes(this.seccionActiva());
  }

  mostrarFlyout(grupo: GrupoSidebarAdminView, event: Event): void {
    if (!this.compacto()) {
      return;
    }
    this.cancelarCierreFlyout();
    const elemento = event.currentTarget as HTMLElement;
    this.flyoutTop.set(elemento.getBoundingClientRect().top);
    this.grupoFlyout.set(grupo);
  }

  programarCierreFlyout(): void {
    this.cancelarCierreFlyout();
    this.cierreFlyoutId = setTimeout(() => this.grupoFlyout.set(null), 140);
  }

  cancelarCierreFlyout(): void {
    if (this.cierreFlyoutId) {
      clearTimeout(this.cierreFlyoutId);
      this.cierreFlyoutId = null;
    }
  }

  seleccionarDesdeFlyout(seccion: SeccionAdmin): void {
    this.seleccionar.emit(seccion);
    this.grupoFlyout.set(null);
  }
}
