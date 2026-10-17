import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface BusinessPolicy {
  nombreComercial: string; version: number; vigenteDesde: string; modoNoShow: string;
  diasReagendar: number | null; diasAclaracion: number; reagendadosMax: number | null;
  textoGenerado: string; textoAdicional: string | null;
  contactoReclamos: { correo: string | null; telefono: string | null };
}
export interface PolicyDraft {
  versionBase: number; modoNoShow: string; diasReagendar: number | null; diasAclaracion: number;
  reagendadosMax: number | null; recordatorio1Dias: number | null; recordatorio2Dias: number | null;
  textoAdicional: string | null; contactoReclamosCorreo: string | null; contactoReclamosTelefono: string | null;
}
export interface AdminPolicy { vigente: BusinessPolicy | null; borrador: PolicyDraft; textoBorrador: string; avisoCobro?: string | null; modos: {codigo: string; etiqueta: string}[]; }
@Injectable({providedIn: 'root'})
export class BusinessPolicyService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;
  publicPolicy(slug: string) { return this.http.get<BusinessPolicy>(`${this.base}/publico/empresas/${encodeURIComponent(slug)}/politicas`); }
  admin() { return this.http.get<AdminPolicy>(`${this.base}/admin/politica`); }
  preview(draft: PolicyDraft) { return this.http.post<{texto: string}>(`${this.base}/admin/politica/vista-previa`, draft); }
  publish(draft: PolicyDraft) { return this.http.post<AdminPolicy>(`${this.base}/admin/politica/versiones`, draft); }
}
