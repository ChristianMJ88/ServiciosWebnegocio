import { Component, OnInit, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PerfilUsuarioLocal } from '../../core/profile/user-profile.service';

@Component({
  selector: 'app-user-profile-dialog',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './user-profile-dialog.component.html',
  styleUrl: './user-profile-dialog.component.css'
})
export class UserProfileDialogComponent implements OnInit {
  readonly profile = input.required<PerfilUsuarioLocal>();
  readonly initials = input.required<string>();
  readonly contextLabel = input('panel');
  readonly saved = output<PerfilUsuarioLocal>();
  readonly cancelled = output<void>();
  readonly validationError = output<string>();

  form: PerfilUsuarioLocal = { nombre: '', puesto: '', fotoDataUrl: null };

  ngOnInit(): void {
    this.form = { ...this.profile() };
  }

  save(): void {
    this.saved.emit({
      nombre: this.form.nombre.trim(),
      puesto: this.form.puesto.trim(),
      fotoDataUrl: this.form.fotoDataUrl
    });
  }

  selectPhoto(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    const file = inputElement.files?.[0];
    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/') || file.size > 1_500_000) {
      this.validationError.emit('Selecciona una imagen válida menor a 1.5 MB.');
      inputElement.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.form = {
        ...this.form,
        fotoDataUrl: typeof reader.result === 'string' ? reader.result : null
      };
    };
    reader.readAsDataURL(file);
  }

  removePhoto(): void {
    this.form = { ...this.form, fotoDataUrl: null };
  }
}
