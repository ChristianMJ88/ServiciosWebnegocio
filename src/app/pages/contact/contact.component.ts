import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.css'
})
export class ContactComponent {
  contactData = {
    fullName: '',
    phone: '',
    email: '',
    subject: '',
    message: ''
  };

  sendEmail() {
    const { fullName, phone, email, subject, message } = this.contactData;

    const mailSubject = encodeURIComponent(subject || 'Contacto desde Web - NailArt Studio');
    const mailBody = encodeURIComponent(
      `Nombre completo: ${fullName}\n` +
      `Teléfono: ${phone}\n` +
      `Correo electrónico: ${email}\n` +
      `Asunto: ${subject}\n\n` +
      `Mensaje:\n${message}`
    );

    const mailtoLink = `christianmejia@techprotech.com.mx?subject=${mailSubject}&body=${mailBody}`;
    window.location.href = mailtoLink;
  }
}
