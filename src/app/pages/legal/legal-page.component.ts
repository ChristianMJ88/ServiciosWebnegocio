import { PlatformContactService } from '../../core/marketing/platform-contact.service';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';

type LegalDocument = 'privacy' | 'terms' | 'deletion';

@Component({
  selector: 'app-legal-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './legal-page.component.html',
  styleUrl: './legal-page.component.css'
})
export class LegalPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);

  readonly contact = signal<string | null>(null);
  readonly contactLoading = signal(true);
  readonly deletionSubject = encodeURIComponent('Solicitud de eliminación de datos');
  private readonly contactApi = inject(PlatformContactService);
  document: LegalDocument = 'privacy';

  ngOnInit(): void {
    this.contactApi.legalContact().subscribe({next: value => {this.contact.set(value.correo);this.contactLoading.set(false);},error: () => this.contactLoading.set(false)});
    this.document = (this.route.snapshot.data['legalDocument'] as LegalDocument | undefined) ?? 'privacy';
    const pageTitle = {
      privacy: 'Aviso de privacidad | Fluora',
      terms: 'Términos y condiciones | Fluora',
      deletion: 'Eliminación de datos | Fluora'
    }[this.document];

    this.title.setTitle(pageTitle);
    this.meta.updateTag({
      name: 'description',
      content: 'Información legal y de protección de datos de la plataforma Fluora.'
    });
  }
}
