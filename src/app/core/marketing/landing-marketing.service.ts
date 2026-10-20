import { EMPTY, shareReplay } from 'rxjs';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
export interface LandingBlock { titulo: string; descripcion: string; }
export interface LandingQuestion { pregunta: string; respuesta: string; }
export interface MarketingLanding {
 slug: string; publicado: boolean; version: number | null;
 etiqueta: string; titulo: string; descripcion: string;
 ctaPrincipal: string; ctaSecundario: string; tituloFinal: string;
 imagenUrl: string | null; imagenAlt: string | null;
 seoTitulo: string; seoDescripcion: string;
 beneficios: LandingBlock[]; funcionalidades: LandingBlock[]; pasos: LandingBlock[];
 preguntas: LandingQuestion[];
}
@Injectable({providedIn:'root'})
export class LandingMarketingService {
 private readonly browser=isPlatformBrowser(inject(PLATFORM_ID));
 private readonly http=inject(HttpClient);
 private navRequest?:ReturnType<LandingMarketingService['requestNavigation']>;
 private navExpires=0;
 private requestNavigation(){return this.http.get<{slug:string;titulo:string;descripcion:string}[]>(environment.apiBaseUrl+'/publico/marketing/landings/navegacion').pipe(shareReplay({bufferSize:1,refCount:true}));}
 // No dynamic backend data in the server build; hydration policy belongs to R3.
 navegacion(){if(!this.browser)return EMPTY;if(!this.navRequest||Date.now()>=this.navExpires){this.navRequest=this.requestNavigation();this.navExpires=Date.now()+60_000;}return this.navRequest;}
 publica(slug:string){return this.http.get<MarketingLanding>(environment.apiBaseUrl+'/publico/marketing/landings/'+encodeURIComponent(slug));}
 listar(){return this.http.get<MarketingLanding[]>(environment.apiBaseUrl+'/plataforma/marketing/landings');}
 guardar(landing:MarketingLanding){return this.http.put<MarketingLanding>(environment.apiBaseUrl+'/plataforma/marketing/landings/'+encodeURIComponent(landing.slug),landing);}
}
