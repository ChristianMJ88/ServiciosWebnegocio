package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol_empresa")
public class RolEmpresaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @ManyToOne
    @JoinColumn(name = "rol_base_id")
    private RolEntidad rolBase;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private boolean editable;

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public RolEntidad getRolBase() {
        return rolBase;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public void setRolBase(RolEntidad rolBase) {
        this.rolBase = rolBase;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
