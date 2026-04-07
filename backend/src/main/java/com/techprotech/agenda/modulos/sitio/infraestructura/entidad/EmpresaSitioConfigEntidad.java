package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Column(name = "dominio_principal", length = 255)
    private String dominioPrincipal;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "descripcion_corta", length = 255)
    private String descripcionCorta;

    @Column(name = "color_primario", length = 20)
    private String colorPrimario;

    @Column(name = "color_secundario", length = 20)
    private String colorSecundario;

    @Column(name = "hero_titulo", length = 180)
    private String heroTitulo;

    @Column(name = "hero_subtitulo", length = 500)
    private String heroSubtitulo;

    @Column(name = "whatsapp", length = 30)
    private String whatsapp;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    @Column(name = "facebook_url", length = 255)
    private String facebookUrl;

    @Column(nullable = false, length = 50)
    private String tema;

    @Column(nullable = false)
    private boolean publicado;

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getDominioPrincipal() {
        return dominioPrincipal;
    }

    public void setDominioPrincipal(String dominioPrincipal) {
        this.dominioPrincipal = dominioPrincipal;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getDescripcionCorta() {
        return descripcionCorta;
    }

    public void setDescripcionCorta(String descripcionCorta) {
        this.descripcionCorta = descripcionCorta;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }

    public String getHeroTitulo() {
        return heroTitulo;
    }

    public void setHeroTitulo(String heroTitulo) {
        this.heroTitulo = heroTitulo;
    }

    public String getHeroSubtitulo() {
        return heroSubtitulo;
    }

    public void setHeroSubtitulo(String heroSubtitulo) {
        this.heroSubtitulo = heroSubtitulo;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public boolean isPublicado() {
        return publicado;
    }

    public void setPublicado(boolean publicado) {
        this.publicado = publicado;
    }
}
