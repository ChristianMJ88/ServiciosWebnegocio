import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ContactService } from '../../services/contact.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.css'
})
export class ContactComponent {
  sending = false;
  successMessage = '';
  errorMessage = '';

  contactData = {
    fullName: '',
    phone: '',
    email: '',
    subject: '',
    message: ''
  };

  constructor(private readonly contactService: ContactService) {}

  sendEmail(form: NgForm) {
    if (this.sending || form.invalid) {
      return;
    }

    this.sending = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.contactService
      .sendContact({
        empresaId: environment.empresaId,
        nombreCompleto: this.contactData.fullName.trim(),
        telefono: this.contactData.phone.trim() || null,
        correo: this.contactData.email.trim(),
        asunto: this.contactData.subject.trim(),
        mensaje: this.contactData.message.trim()
      })
      .pipe(finalize(() => (this.sending = false)))
      .subscribe({
        next: response => {
          this.successMessage = response.mensaje || 'Tu mensaje fue enviado correctamente.';
          form.resetForm();
        },
        error: err => {
          this.errorMessage = err?.message || 'No se pudo enviar tu mensaje.';
        }
      });
  }
}
