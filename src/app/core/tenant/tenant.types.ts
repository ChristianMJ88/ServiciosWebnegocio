export interface TenantSiteConfig {
  empresaId: number;
  slug: string;
  nombreComercial: string;
  dominioPrincipal?: string | null;
  logoUrl: string | null;
  descripcionCorta: string | null;
  colorPrimario: string | null;
  colorSecundario: string | null;
  fuenteTitulos: string | null;
  fuenteCuerpo: string | null;
  heroTitulo: string | null;
  heroSubtitulo: string | null;
  heroImagenUrl: string | null;
  whatsapp: string | null;
  telefono: string | null;
  correo: string | null;
  direccion: string | null;
  instagramUrl: string | null;
  facebookUrl: string | null;
  tema: string | null;
  publicado: boolean;
}

export interface TenantContext extends TenantSiteConfig {}
