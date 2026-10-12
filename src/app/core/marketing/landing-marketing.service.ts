import { Injectable, inject } from '@angular/core';
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
 private readonly http=inject(HttpClient);
 navegacion(){return this.http.get<{slug:string;titulo:string;descripcion:string}[]>(environment.apiBaseUrl+'/publico/marketing/landings/navegacion');}
 publica(slug:string){return this.http.get<MarketingLanding>(environment.apiBaseUrl+'/publico/marketing/landings/'+encodeURIComponent(slug));}
 listar(){return this.http.get<MarketingLanding[]>(environment.apiBaseUrl+'/plataforma/marketing/landings');}
 guardar(landing:MarketingLanding){return this.http.put<MarketingLanding>(environment.apiBaseUrl+'/plataforma/marketing/landings/'+encodeURIComponent(landing.slug),landing);}
}
