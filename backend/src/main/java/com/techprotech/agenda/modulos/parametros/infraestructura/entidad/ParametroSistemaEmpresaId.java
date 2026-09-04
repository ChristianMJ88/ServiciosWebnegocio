package com.techprotech.agenda.modulos.parametros.infraestructura.entidad;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ParametroSistemaEmpresaId implements Serializable {
    @Column(name = "empresa_id") private Long empresaId;
    @Column(name = "clave", length = 80) private String clave;
    public ParametroSistemaEmpresaId() {}
    public ParametroSistemaEmpresaId(Long empresaId, String clave) { this.empresaId = empresaId; this.clave = clave; }
    public Long getEmpresaId() { return empresaId; }
    public String getClave() { return clave; }
    @Override public boolean equals(Object o) { return o instanceof ParametroSistemaEmpresaId that && Objects.equals(empresaId, that.empresaId) && Objects.equals(clave, that.clave); }
    @Override public int hashCode() { return Objects.hash(empresaId, clave); }
}
