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

  @Column(name = "inicio_titulo_servicios", length = 180)
  private String inicioTituloServicios;

  @Column(name = "inicio_descripcion_servicios", length = 500)
  private String inicioDescripcionServicios;

  @Column(name = "inicio_titulo_contacto", length = 180)
  private String inicioTituloContacto;

  @Column(name = "inicio_descripcion_contacto", length = 500)
  private String inicioDescripcionContacto;

  @Column(name = "whatsapp", length = 30)
  private String whatsapp;

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

  @Column(nullable = false, length = 50)
  private String tema;

  @Column(nullable = false)
  private boolean publicado;


}
