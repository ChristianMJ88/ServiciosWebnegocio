import { ChangeDetectionStrategy, Component, input, inject } from '@angular/core';
import { MarketingOfferService } from '../../../core/marketing/marketing-offer.service';
import { PlatformHostService } from '../../../core/platform/platform-host.service';
import { HOME_CONTENT } from '../fluora-home.content';
@Component({selector:'app-home-hero',standalone:true,templateUrl:'./hero.component.html',styleUrl:'./hero.component.css',changeDetection:ChangeDetectionStrategy.OnPush})
export class HomeHeroComponent {
  readonly content=HOME_CONTENT.hero;
  readonly offer=inject(MarketingOfferService);
  readonly host=inject(PlatformHostService);
  readonly verticals=input<{slug:string;titulo:string;descripcion:string}[]>([]);
  href(slug:string){return this.host.marketingUrl('/'+encodeURIComponent(slug));}
  label(title:string){return title.replace(/^Fluora para\s+/i,'');}
}
