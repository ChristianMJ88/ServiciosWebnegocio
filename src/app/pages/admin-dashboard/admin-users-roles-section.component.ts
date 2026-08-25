
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import {
  GuardarRolInternoPayload,
  PlantillaRolInternoAdmin,
  RolInternoAdmin
} from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-users-roles-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule
],
  templateUrl: './admin-users-roles-section.component.html'
})
export class AdminUsersRolesSectionComponent {
  @Input({ required: true }) rolInternoEditandoId!: number | null;
  @Input({ required: true }) rolInternoClonandoDesdeId!: number | null;
  @Input({ required: true }) rolInternoClonandoNombreOrigen!: string | null;
  @Input({ required: true }) plantillasRolesInternos!: PlantillaRolInternoAdmin[];
  @Input({ required: true }) formularioRolInterno!: GuardarRolInternoPayload;
  @Input({ required: true }) gruposPermisos!: Array<{
    id: string;
    titulo: string;
    permisos: Array<{ codigo: string; nombre: string }>;
  }>;
  @Input({ required: true }) rolesInternos!: RolInternoAdmin[];
  @Input({ required: true }) permisoRolSeleccionado!: (codigo: string) => boolean;

  @Output() clearEdit = new EventEmitter<void>();
  @Output() useTemplate = new EventEmitter<PlantillaRolInternoAdmin>();
  @Output() save = new EventEmitter<void>();
  @Output() rolePermissionChange = new EventEmitter<{ codigo: string; checked: boolean }>();
  @Output() cloneRole = new EventEmitter<RolInternoAdmin>();
  @Output() editRole = new EventEmitter<RolInternoAdmin>();
  @Output() deleteRole = new EventEmitter<RolInternoAdmin>();
}
