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
  heroEtiqueta?: string | null;
  inicioTituloServicios?: string | null;
  inicioDescripcionServicios?: string | null;
  inicioTituloContacto?: string | null;
  inicioDescripcionContacto?: string | null;
  whatsapp: string | null;
  telefono: string | null;
  correo: string | null;
  direccion: string | null;
  instagramUrl: string | null;
  facebookUrl: string | null;
  xUrl?: string | null;
  tiktokUrl?: string | null;
  whatsappHabilitado?: boolean;
  instagramHabilitado?: boolean;
  facebookHabilitado?: boolean;
  xHabilitado?: boolean;
  tiktokHabilitado?: boolean;
  tema: string | null;
  publicado: boolean;
}

export interface TenantContext extends TenantSiteConfig {}
