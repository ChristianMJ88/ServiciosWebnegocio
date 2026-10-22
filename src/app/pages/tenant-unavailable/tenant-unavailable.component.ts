import { Component, inject } from '@angular/core';
import { SeoService } from '../../core/seo/seo.service';
import { PlatformHostService } from '../../core/platform/platform-host.service';
@Component({selector:'app-tenant-unavailable',standalone:true,
 template:`<main aria-labelledby="unavailable-title"><h1 id="unavailable-title">Este sitio no está disponible</h1><p>Intenta de nuevo más tarde o contacta al negocio.</p><a href="/">Volver a intentar</a><a [href]="marketing">Volver a Fluora</a></main>`,
 styles:`main{max-width:32rem;margin:clamp(2rem,10vh,6rem) auto;padding:1.5rem;display:grid;gap:1rem}h1,p{margin:0}a{min-height:44px;display:flex;align-items:center}`})
export class TenantUnavailableComponent {
 readonly marketing=inject(PlatformHostService).marketingUrl('/');
 constructor(){inject(SeoService).applyUnavailableMarketing();}
}
