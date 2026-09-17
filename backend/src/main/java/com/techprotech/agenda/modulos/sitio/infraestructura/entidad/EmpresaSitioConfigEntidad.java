package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "empresa_sitio_config")
public class EmpresaSitioConfigEntidad {

  @Id
  @Column(name = "empresa_id")
  private Long empresaId;

  @Column(nullable = false, unique = true, length = 100)
  private String slug;

  @Column(name = "nombre_comercial", nullable = false, length = 150)
  private String nombreComercial;

  @Column(name = "dominio_principal")
  private String dominioPrincipal;

  @Column(name = "logo_url")
  private String logoUrl;

  @Column(name = "descripcion_corta")
  private String descripcionCorta;

  @Column(name = "color_primario", length = 20)
  private String colorPrimario;

  @Column(name = "color_secundario", length = 20)
  private String colorSecundario;

  @Column(name = "fuente_titulos", length = 40)
  private String fuenteTitulos;

  @Column(name = "fuente_cuerpo", length = 40)
  private String fuenteCuerpo;

  @Column(name = "hero_titulo", length = 180)
  private String heroTitulo;

  @Column(name = "hero_subtitulo", length = 500)
  private String heroSubtitulo;

  @Column(name = "hero_imagen_url", length = 500)
  private String heroImagenUrl;

  @Column(name = "hero_etiqueta", length = 80)
  private String heroEtiqueta;

  @Column(name = "hero_alineacion", nullable = false, length = 20)
  private String heroAlineacion = "IZQUIERDA";

  @Column(name = "hero_overlay", nullable = false, length = 20)
  private String heroOverlay = "MEDIO";

  @Column(name = "hero_texto_cta", length = 40)
  private String heroTextoCta;

  @Column(name = "hero_accion_cta", nullable = false, length = 20)
  private String heroAccionCta = "AGENDAR";

  @Column(name = "hero_mostrar_whatsapp", nullable = false)
  private boolean heroMostrarWhatsapp = true;

  @Column(name = "inicio_titulo_servicios", length = 180)
  private String inicioTituloServicios;

  @Column(name = "inicio_descripcion_servicios", length = 500)
  private String inicioDescripcionServicios;

  @Column(name = "inicio_etiqueta_servicios", length = 40)
  private String inicioEtiquetaServicios;

  @Column(name = "inicio_texto_ver_servicios", length = 40)
  private String inicioTextoVerServicios;

  @Column(name = "inicio_texto_reservar", length = 40)
  private String inicioTextoReservar;

  @Column(name = "inicio_titulo_contacto", length = 180)
  private String inicioTituloContacto;

  @Column(name = "inicio_descripcion_contacto", length = 500)
  private String inicioDescripcionContacto;

  @Column(name = "inicio_etiqueta_contacto", length = 40)
  private String inicioEtiquetaContacto;

  @Column(name = "inicio_texto_agenda", length = 40)
  private String inicioTextoAgenda;

  @Column(name = "inicio_texto_whatsapp", length = 40)
  private String inicioTextoWhatsapp;

  @Column(name = "inicio_mostrar_contacto", nullable = false)
  private boolean inicioMostrarContacto = true;

  @Column(name = "inicio_mostrar_servicios", nullable = false)
  private boolean inicioMostrarServicios = true;

  @Column(name = "inicio_orden_secciones", nullable = false, length = 30)
  private String inicioOrdenSecciones = "CONTACTO_SERVICIOS";

  @Column(name = "seo_titulo", length = 70)
  private String seoTitulo;

  @Column(name = "seo_descripcion", length = 170)
  private String seoDescripcion;

  @Column(name = "whatsapp", length = 30)
  private String whatsapp;

  @Column(name = "whatsapp_mensaje", length = 255)
  private String whatsappMensaje;

  @Column(name = "telefono", length = 30)
  private String telefono;

  @Column(name = "correo", length = 150)
  private String correo;

  @Column(name = "direccion")
  private String direccion;

  @Column(name = "instagram_url")
  private String instagramUrl;

  @Column(name = "facebook_url")
  private String facebookUrl;

  @Column(name = "x_url")
  private String xUrl;

  @Column(name = "tiktok_url")
  private String tiktokUrl;

  @Column(name = "whatsapp_habilitado", nullable = false)
  private boolean whatsappHabilitado = true;

  @Column(name = "instagram_habilitado", nullable = false)
  private boolean instagramHabilitado = true;

  @Column(name = "facebook_habilitado", nullable = false)
  private boolean facebookHabilitado = true;

  @Column(name = "x_habilitado", nullable = false)
  private boolean xHabilitado = true;

  @Column(name = "tiktok_habilitado", nullable = false)
  private boolean tiktokHabilitado = true;

  @Column(name = "chat_habilitado", nullable = false)
  private boolean chatHabilitado = true;

  @Column(name = "chat_titulo", length = 80)
  private String chatTitulo;

  @Column(name = "chat_saludo", length = 255)
  private String chatSaludo;

  @Column(nullable = false, length = 50)
  private String tema;

  @Column(name = "menu_estilo", nullable = false, length = 20)
  private String menuEstilo = "FLOTANTE";

  @Column(name = "menu_mostrar_inicio", nullable = false)
  private boolean menuMostrarInicio = true;

  @Column(name = "menu_mostrar_servicios", nullable = false)
  private boolean menuMostrarServicios = true;

  @Column(name = "menu_mostrar_contacto", nullable = false)
  private boolean menuMostrarContacto = true;

  @Column(name = "menu_mostrar_registro", nullable = false)
  private boolean menuMostrarRegistro = true;

  @Column(name = "menu_mostrar_descripcion", nullable = false)
  private boolean menuMostrarDescripcion = true;

  @Column(name = "menu_texto_cta", length = 40)
  private String menuTextoCta;

  @Column(name = "menu_accion_cta", nullable = false, length = 20)
  private String menuAccionCta = "AGENDAR";

  @Column(name = "footer_texto", length = 300)
  private String footerTexto;

  @Column(name = "footer_mostrar_navegacion", nullable = false)
  private boolean footerMostrarNavegacion = true;

  @Column(name = "footer_mostrar_contacto", nullable = false)
  private boolean footerMostrarContacto = true;

  @Column(nullable = false)
  private boolean publicado;


}
