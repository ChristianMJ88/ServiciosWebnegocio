package com.techprotech.agenda.modulos.parametros.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name = "parametro_sistema_empresa")
public class ParametroSistemaEmpresaEntidad {
    @EmbeddedId private ParametroSistemaEmpresaId id;
    @Column(nullable = false, length = 500) private String valor;
    @Column(name = "actualizado_en", insertable = false, updatable = false) private LocalDateTime actualizadoEn;
    @Column(name = "actualizado_por_usuario_id") private Long actualizadoPorUsuarioId;
}
