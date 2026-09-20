package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import com.techprotech.agenda.modulos.sitio.dominio.AlineacionBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.AnchoBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.EspaciadoBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.PresentacionBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.SeparadorBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.SuperficieBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.TipoBloqueSitio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "empresa_sitio_bloque",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_empresa_sitio_bloque_tipo", columnNames = {"empresa_id", "tipo"}),
                @UniqueConstraint(name = "uk_empresa_sitio_bloque_orden", columnNames = {"empresa_id", "orden"})
        }
)
public class EmpresaSitioBloqueEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoBloqueSitio tipo;

    @Column(nullable = false)
    private boolean habilitado;

    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SuperficieBloqueSitio superficie = SuperficieBloqueSitio.BASE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresentacionBloqueSitio presentacion = PresentacionBloqueSitio.ABIERTA;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancho_contenido", nullable = false, length = 20)
    private AnchoBloqueSitio anchoContenido = AnchoBloqueSitio.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlineacionBloqueSitio alineacion = AlineacionBloqueSitio.IZQUIERDA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeparadorBloqueSitio separador = SeparadorBloqueSitio.NINGUNO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EspaciadoBloqueSitio espaciado = EspaciadoBloqueSitio.NORMAL;

    @Column(length = 80)
    private String etiqueta;

    @Column(length = 160)
    private String titulo;

    @Column(name = "descripcion_visual", length = 400)
    private String descripcionVisual;
}
