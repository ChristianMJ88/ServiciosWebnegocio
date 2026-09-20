import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TenantTeamMember } from '../../../core/tenant/tenant.types';

@Component({
  selector: 'app-public-team-card',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './public-team-card.component.html',
  styleUrl: './public-team-card.component.css'
})
export class PublicTeamCardComponent {
  @Input({ required: true }) member!: TenantTeamMember;
  @Input({ required: true }) bookingLink = '';

  initials(): string {
    return this.member.nombre
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(fragment => fragment.charAt(0).toUpperCase())
      .join('');
  }
}
