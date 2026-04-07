import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { AuditoriaRolInternoAdmin } from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-users-activity-section',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule
  ],
  templateUrl: './admin-users-activity-section.component.html'
})
export class AdminUsersActivitySectionComponent {
  @Input({ required: true }) auditoriaRolesInternos!: AuditoriaRolInternoAdmin[];
  @Input({ required: true }) etiquetaAccionAuditoriaRol!: (accion: string) => string;
}
