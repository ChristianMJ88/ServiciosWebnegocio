import { ChangeDetectionStrategy, Component, input, inject, computed, signal } from '@angular/core';
import { MarketingOfferService } from '../../../core/marketing/marketing-offer.service';
import { PublicSupport } from '../../../core/marketing/platform-contact.service';
import { supportSchedule } from '../../../core/marketing/support-format';
import { HOME_CONTENT } from '../fluora-home.content';
@Component({selector:'app-home-faq',standalone:true,templateUrl:'./faq.component.html',styleUrl:'./faq.component.css',changeDetection:ChangeDetectionStrategy.OnPush})
export class HomeFaqComponent {
  readonly support=input<PublicSupport|null>(null);
  readonly schedule=supportSchedule;
  readonly open=signal<string|null>(null);
  readonly offer=inject(MarketingOfferService);
  readonly faqs=computed(()=>HOME_CONTENT.faq.map(item=>({...item,answer:item.answer+(item.id==='precio'&&this.offer.recommended()?.diasPrueba?` La prueba vigente de ${this.offer.recommended()!.nombre} es de ${this.offer.recommended()!.diasPrueba} días, sin tarjeta y sujeta a elegibilidad.`:'')})));
  toggle(id:string){this.open.update(current=>current===id?null:id);}
}
