import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-public-section-heading',
  standalone: true,
  template: `
    <header class="section-heading">
      @if (eyebrow()) {<span class="section-heading__eyebrow">{{ eyebrow() }}</span>}
      <h2>{{ title() }}</h2>
      @if (description()) {<p>{{ description() }}</p>}
    </header>
  `,
  styleUrl: './public-section-heading.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PublicSectionHeadingComponent {
  readonly eyebrow = input<string | null | undefined>();
  readonly title = input.required<string>();
  readonly description = input<string | null | undefined>();
}
