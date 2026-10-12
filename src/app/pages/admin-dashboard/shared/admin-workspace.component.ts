import { Component, effect, input, signal } from '@angular/core';

/** Presentation only: projected forms keep their own validation and API calls. */
@Component({
  selector: 'app-admin-workspace',
  standalone: true,
  template: `
    <nav class="workspace-views" [class.always-visible]="separado()" aria-label="Vista del módulo">
      <button type="button" [attr.aria-pressed]="vista() === 'lista'" (click)="vista.set('lista')"><i class="bi bi-list-ul" aria-hidden="true"></i> {{ tituloLista() }}</button>
      <button type="button" [attr.aria-pressed]="vista() === 'formulario'" (click)="vista.set('formulario')"><i class="bi bi-pencil-square" aria-hidden="true"></i> {{ editando() ? 'Editar' : tituloFormulario() }}</button>
    </nav>
    <div class="workspace-grid two-columns" [class.separated]="separado()" [style.grid-template-columns]="separado() ? 'minmax(0, 1fr)' : null">
      <div class="workspace-pane" [class.mobile-hidden]="vista() !== 'formulario'"><ng-content select=".form-card" /></div>
      <div class="workspace-pane" [class.mobile-hidden]="vista() !== 'lista'"><ng-content select=".list-card" /></div>
    </div>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .workspace-pane { min-width: 0; }
    .workspace-views { display: none; }
    .workspace-views.always-visible { display:flex; gap:.3rem; padding:.3rem; margin-bottom:1rem; border-radius:14px; background:#e6edf6; width:100%; max-width:520px; }
    .workspace-views.always-visible button { flex:1; min-height:46px; border:0; border-radius:11px; color:#52647b; background:transparent; font:inherit; font-weight:600; }
    .workspace-views.always-visible button[aria-pressed=true] { color:#235baa; background:#fff; box-shadow:0 2px 8px #16334b12; }
    .separated .mobile-hidden { display:none; }
    @media(max-width:767.98px) {
      .workspace-views { display:flex; gap:.3rem; padding:.3rem; margin-bottom:1rem; border-radius:14px; background:#e6edf6; }
      .workspace-views button { flex:1; min-height:46px; border:0; border-radius:11px; color:#52647b; background:transparent; font:inherit; font-weight:600; }
      .workspace-views button[aria-pressed=true] { color:#235baa; background:#fff; box-shadow:0 2px 8px #16334b12; }
      .workspace-views button:focus-visible { outline:2px solid #4379ff; outline-offset:2px; }
      .mobile-hidden { display:none; }
    }
  `
})
export class AdminWorkspaceComponent {
  readonly editando = input<number | string | null>(null);
  readonly separado = input(false);
  readonly tituloLista = input('Registros');
  readonly tituloFormulario = input('Crear');
  readonly vista = signal<'lista' | 'formulario'>('lista');

  constructor() {
    let edicionAnterior: number | string | null = null;
    effect(() => {
      const edicion = this.editando();
      if (edicion) this.vista.set('formulario');
      else if (edicionAnterior) this.vista.set('lista');
      edicionAnterior = edicion;
    });
  }
}
