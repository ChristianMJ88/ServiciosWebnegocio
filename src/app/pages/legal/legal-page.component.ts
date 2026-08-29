import { Component, OnInit, inject } from '@angular/core';
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

  document: LegalDocument = 'privacy';

  ngOnInit(): void {
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
