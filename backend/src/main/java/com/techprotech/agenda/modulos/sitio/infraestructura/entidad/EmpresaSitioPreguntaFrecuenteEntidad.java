package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name = "empresa_sitio_pregunta_frecuente")
public class EmpresaSitioPreguntaFrecuenteEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(nullable = false, length = 180) private String pregunta;
    @Column(nullable = false, length = 1200) private String respuesta;
    @Column(nullable = false) private boolean activa;
    @Column(nullable = false) private int orden;
}
