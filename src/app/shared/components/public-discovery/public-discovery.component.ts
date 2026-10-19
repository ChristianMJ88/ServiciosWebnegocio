import { Component, inject, input, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LandingMarketingService } from '../../../core/marketing/landing-marketing.service';
import { PlatformHostService } from '../../../core/platform/platform-host.service';
@Component({selector:'app-public-discovery',standalone:true,imports:[NgTemplateOutlet],host:{'[class.compact]':'compact()'},templateUrl:'./public-discovery.component.html',styleUrl:'./public-discovery.component.css'})
export class PublicDiscoveryComponent {
 readonly compact=input(false);
 private readonly host=inject(PlatformHostService);
 readonly links=signal<{slug:string;titulo:string;descripcion:string}[]>([]);
 readonly directoryHref=this.host.marketingUrl('/negocios');
 href(slug:string){return this.host.marketingUrl('/'+encodeURIComponent(slug));}
 constructor(){inject(LandingMarketingService).navegacion().pipe(takeUntilDestroyed()).subscribe({next:links=>this.links.set(links),error:()=>this.links.set([])});}
}
