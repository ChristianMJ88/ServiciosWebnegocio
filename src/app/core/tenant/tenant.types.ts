export interface TenantSiteBlock {
  tipo: string;
  habilitado: boolean;
  orden: number;
  superficie: 'BASE' | 'SUAVE' | 'MARCA' | 'CONTRASTE';
  presentacion: 'ABIERTA' | 'PANEL' | 'BANDA';
  anchoContenido: 'COMPACTO' | 'NORMAL' | 'AMPLIO';
  alineacion: 'IZQUIERDA' | 'CENTRO';
  separador: 'NINGUNO' | 'LINEA' | 'DEGRADADO';
  espaciado: 'COMPACTO' | 'NORMAL' | 'AMPLIO';
  etiqueta: string | null;
  titulo: string | null;
  descripcion: string | null;
}

export interface TenantFeaturedServiceItem {
  tipo: 'GRUPO' | 'SUBGRUPO' | 'SERVICIO';
  referenciaId: number;
  grupoId: number | null;
  subgrupoId: number | null;
  servicioId: number | null;
  titulo: string;
  descripcion: string | null;
  imagenUrl: string | null;
  precioDesde: number | null;
  moneda: string | null;
  duracionMinutos: number | null;
  cantidadServicios: number;
  etiqueta: string | null;
}

export interface TenantFeaturedServices {
  vista: 'TARJETAS' | 'CARRUSEL' | 'LISTA';
  mostrarPrecio: boolean;
  mostrarDuracion: boolean;
  mostrarDescripcion: boolean;
  limite: number;
  seleccionAutomatica: boolean;
  elementos: TenantFeaturedServiceItem[];
}

export interface TenantPromotion {
  id: number;
  titulo: string;
  descripcion: string | null;
  imagenUrl: string | null;
  fechaInicio: string | null;
  fechaFin: string | null;
  tipoDescuento: 'PORCENTAJE' | 'PRECIO';
  valorDescuento: number;
  moneda: string;
  textoBoton: string;
  servicioIds: number[];
  servicios: string[];
  sucursales: string[];
}

export interface TenantGalleryImage {
  id: number;
  imagenUrl: string;
  imagenUrlPequena?: string;
  imagenUrlMediana?: string;
  imagenUrlGrande?: string;
  imagenUrlAmpliada?: string;
  textoAlternativo: string;
  titulo: string | null;
  descripcion: string | null;
  servicioId: number | null;
  servicioNombre: string | null;
  sucursalId: number | null;
  sucursalNombre: string | null;
}

export interface TenantGallery {
  titulo: string;
  descripcion: string | null;
  vista: 'CUADRICULA' | 'MOSAICO' | 'CARRUSEL';
  ampliacionHabilitada: boolean;
  imagenes: TenantGalleryImage[];
}

export interface TenantBenefit {
  id: number;
  titulo: string;
  descripcion: string;
  icono: 'ESTRELLA' | 'CORAZON' | 'ESCUDO' | 'DESTELLO' | 'RELOJ' | 'PERSONA' | 'HOJA' | 'PREMIO';
}

export interface TenantTeamMember {
  id: number;
  nombre: string;
  biografia: string | null;
  fotoUrl: string | null;
  sucursalNombre: string;
}

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
  heroTipoMedio?: string | null;
  heroVideoUrl?: string | null;
  heroAltura?: string | null;
  heroEtiqueta?: string | null;
  heroAlineacion?: string | null;
  heroOverlay?: string | null;
  heroTextoCta?: string | null;
  heroAccionCta?: string | null;
  heroEnlaceCta?: string | null;
  heroTextoSecundario?: string | null;
  heroAccionSecundaria?: string | null;
  heroEnlaceSecundario?: string | null;
  heroMostrarSecundario?: boolean;
  heroMostrarWhatsapp?: boolean;
  inicioTituloServicios?: string | null;
  inicioDescripcionServicios?: string | null;
  inicioEtiquetaServicios?: string | null;
  inicioTextoVerServicios?: string | null;
  inicioTextoReservar?: string | null;
  inicioTituloContacto?: string | null;
  inicioDescripcionContacto?: string | null;
  inicioEtiquetaContacto?: string | null;
  inicioTextoAgenda?: string | null;
  inicioTextoWhatsapp?: string | null;
  inicioMostrarContacto?: boolean;
  inicioMostrarServicios?: boolean;
  inicioOrdenSecciones?: string | null;
  seoTitulo?: string | null;
  seoDescripcion?: string | null;
  whatsapp: string | null;
  whatsappMensaje?: string | null;
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
  chatHabilitado?: boolean;
  chatTitulo?: string | null;
  chatSaludo?: string | null;
  tema: string | null;
  menuEstilo?: string | null;
  menuMostrarInicio?: boolean;
  menuMostrarServicios?: boolean;
  menuMostrarContacto?: boolean;
  menuMostrarRegistro?: boolean;
  menuMostrarDescripcion?: boolean;
  menuTextoCta?: string | null;
  menuAccionCta?: string | null;
  footerTexto?: string | null;
  footerMostrarNavegacion?: boolean;
  footerMostrarContacto?: boolean;
  bloques?: TenantSiteBlock[];
  serviciosDestacados?: TenantFeaturedServices | null;
  galeria?: TenantGallery | null;
  promociones?: TenantPromotion[];
  beneficios?: TenantBenefit[];
  equipo?: TenantTeamMember[];
  publicado: boolean;
}

export interface TenantContext extends TenantSiteConfig {}
