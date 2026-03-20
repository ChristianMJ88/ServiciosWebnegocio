package com.techprotech.agenda.modulos.prestadores.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prestador_servicio")
public class PrestadorServicioEntidad {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    @Column(name = "nombre_mostrar", nullable = false, length = 150)
    private String nombreMostrar;

    @Column(length = 500)
    private String biografia;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "color_agenda", length = 20)
    private String colorAgenda;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public String getNombreMostrar() {
        return nombreMostrar;
    }

    public void setNombreMostrar(String nombreMostrar) {
        this.nombreMostrar = nombreMostrar;
    }

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getColorAgenda() {
        return colorAgenda;
    }

    public void setColorAgenda(String colorAgenda) {
        this.colorAgenda = colorAgenda;
    }
}
