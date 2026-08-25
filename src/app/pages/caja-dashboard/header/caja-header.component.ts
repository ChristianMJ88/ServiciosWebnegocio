import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { PerfilUsuarioLocal } from '../../../core/profile/user-profile.service';
import { UserProfileDialogComponent } from '../../../shared/profile/user-profile-dialog.component';
import { CajaNotificationViewModel } from '../models/caja-dashboard.models';

@Component({
  selector: 'app-caja-header',
  standalone: true,
  imports: [CommonModule, MatBadgeModule, MatButtonModule, MatMenuModule, MatToolbarModule, UserProfileDialogComponent],
  templateUrl: './caja-header.component.html',
  styleUrls: ['./caja-header.component.css']
})
export class CajaHeaderComponent {
  @Input({ required: true }) panelMovil!: boolean;
  @Input({ required: true }) puedeIrRecepcion!: boolean;
  @Input({ required: true }) puedeIrAdmin!: boolean;
  @Input({ required: true }) notificaciones!: CajaNotificationViewModel[];
  @Input({ required: true }) totalNotificaciones!: number;
  @Input({ required: true }) nombreUsuario!: string;
  @Input({ required: true }) correoUsuario!: string;
  @Input({ required: true }) puestoUsuario!: string;
  @Input({ required: true }) fotoPerfilUsuario!: string | null;
  @Input({ required: true }) inicialesUsuario!: string;
  @Input({ required: true }) perfilConfigAbierto!: boolean;

  @Output() refresh = new EventEmitter<void>();
  @Output() openProfile = new EventEmitter<void>();
  @Output() closeProfile = new EventEmitter<void>();
  @Output() saveProfile = new EventEmitter<PerfilUsuarioLocal>();
  @Output() profileValidationError = new EventEmitter<string>();
  @Output() goReception = new EventEmitter<void>();
  @Output() goAdmin = new EventEmitter<void>();
  @Output() signOut = new EventEmitter<void>();
}
