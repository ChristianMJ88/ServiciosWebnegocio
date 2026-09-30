package com.techprotech.agenda.modulos.envios.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "envio_evento")
public class EnvioEventoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "envio_pedido_id", nullable = false) private Long envioPedidoId;
    @Column(nullable = false, length = 50) private String tipo;
    @Column(nullable = false, length = 40) private String estado;
    @Column(length = 500) private String descripcion;
    @Column(name = "referencia_externa", length = 190) private String referenciaExterna;
    @Column(name = "creado_en", insertable = false, updatable = false) private LocalDateTime creadoEn;
}
