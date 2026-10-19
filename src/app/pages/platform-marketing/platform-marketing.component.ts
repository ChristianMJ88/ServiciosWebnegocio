import { SupportEditorComponent } from './support-editor.component';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LandingMarketingService, MarketingLanding } from '../../core/marketing/landing-marketing.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
@Component({selector:'app-platform-marketing',standalone:true,imports:[FormsModule,RouterLink,SupportEditorComponent],
 templateUrl:'./platform-marketing.component.html',styleUrl:'./platform-marketing.component.css'})
export class PlatformMarketingComponent {
 private readonly api=inject(LandingMarketingService);
 readonly host=inject(PlatformHostService);
 readonly landings=signal<MarketingLanding[]>([]);
 readonly loading=signal(true);
 readonly saving=signal(false);
 readonly error=signal('');
 readonly message=signal('');
 draft:MarketingLanding|null=null;
 constructor(){this.api.listar().pipe(takeUntilDestroyed()).subscribe({next:l=>{this.landings.set(l);this.loading.set(false);},error:()=>{this.error.set('No fue posible acceder al contenido. Verifica tu sesión y permisos de plataforma.');this.loading.set(false);}});}
 editar(l:MarketingLanding){this.draft=structuredClone(l);this.error.set('');this.message.set('');}
 guardar(){
  if(!this.draft||this.saving())return;
  this.saving.set(true);this.error.set('');this.message.set('');
  this.api.guardar(this.draft).subscribe({next:l=>{this.landings.update(all=>all.map(i=>i.slug===l.slug?l:i));this.draft=structuredClone(l);this.saving.set(false);this.message.set('Contenido guardado.');},error:e=>{this.saving.set(false);this.error.set(e.error?.mensaje||'No fue posible guardar. Actualiza el contenido e inténtalo de nuevo.');}});
 }
}
