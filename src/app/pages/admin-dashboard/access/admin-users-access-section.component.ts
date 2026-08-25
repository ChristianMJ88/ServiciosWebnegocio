
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import {
  GuardarUsuarioInternoPayload,
  RolInternoAdmin,
  SucursalAdmin,
  UsuarioInternoAdmin
} from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-users-access-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule
],
  templateUrl: './admin-users-access-section.component.html'
})
export class AdminUsersAccessSectionComponent {
  @Input({ required: true }) usuarioInternoEditandoId!: number | null;
  @Input({ required: true }) formularioUsuarioInterno!: GuardarUsuarioInternoPayload;
  @Input({ required: true }) sucursales!: SucursalAdmin[];
  @Input({ required: true }) rolesUsuarioInterno!: RolInternoAdmin[];
  @Input({ required: true }) rolUsuarioInternoSeleccionado!: RolInternoAdmin | null;
  @Input({ required: true }) resumenAccesoUsuarioInterno!: {
    rolNombre: string;
    permisosRol: number;
    permisosDirectos: number;
    permisosEfectivos: number;
    scopeTexto: string;
  };
  @Input({ required: true }) permisosDirectosUsuarioInterno!: string[];
  @Input({ required: true }) permisosEfectivosUsuarioInterno!: string[];
  @Input({ required: true }) gruposPermisosDirectosUsuarioInterno!: Array<{
    id: string;
    titulo: string;
    permisos: Array<{ codigo: string; nombre: string }>;
  }>;
  @Input({ required: true }) usuariosInternosFiltrados!: UsuarioInternoAdmin[];
  @Input({ required: true }) filtroUsuariosInternos!: string;
  @Input({ required: true }) etiquetaPermiso!: (permiso: string) => string;
  @Input({ required: true }) resumenPermisosLista!: (permisos: string[], limite?: number) => string;
  @Input({ required: true }) resumenPermisosRol!: (rol: RolInternoAdmin, limite?: number) => string;
  @Input({ required: true }) rolInternoDeUsuario!: (usuario: UsuarioInternoAdmin) => RolInternoAdmin | null;
  @Input({ required: true }) permisoDirectoUsuarioSeleccionado!: (codigo: string) => boolean;

  @Output() save = new EventEmitter<void>();
  @Output() clearEdit = new EventEmitter<void>();
  @Output() branchScopeChange = new EventEmitter<number[]>();
  @Output() roleChange = new EventEmitter<number | null>();
  @Output() editSelectedRole = new EventEmitter<void>();
  @Output() directPermissionChange = new EventEmitter<{ codigo: string; checked: boolean }>();
  @Output() filterChange = new EventEmitter<string>();
  @Output() clearFilter = new EventEmitter<void>();
  @Output() editAccess = new EventEmitter<UsuarioInternoAdmin>();
}
