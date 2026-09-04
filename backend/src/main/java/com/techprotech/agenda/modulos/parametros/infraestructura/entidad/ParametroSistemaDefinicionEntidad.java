package com.techprotech.agenda.modulos.parametros.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name = "parametro_sistema_definicion")
public class ParametroSistemaDefinicionEntidad {
    @Id @Column(length = 80) private String clave;
    @Column(nullable = false, length = 120) private String nombre;
    @Column(length = 255) private String descripcion;
    @Column(nullable = false, length = 20) private String tipo;
    @Column(name = "valor_predeterminado", nullable = false, length = 500) private String valorPredeterminado;
    @Column(nullable = false, length = 80) private String categoria;
    @Column(name = "opciones_json", columnDefinition = "json") private String opcionesJson;
    @Column(nullable = false) private int orden;
    @Column(nullable = false) private boolean editable;
}
