package com.techprotech.agenda.modulos.plataforma.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name="plataforma_stripe_evento")
public class StripeEventoPlataformaEntidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="stripe_evento_id",nullable=false,unique=true,length=100) private String stripeEventoId;
    @Column(nullable=false,length=120) private String tipo;
    @Column(name="modo_produccion",nullable=false) private boolean modoProduccion;
    @Column(nullable=false,length=30) private String estado;
    @Column(name="error_ultimo",length=1000) private String errorUltimo;
    @Column(name="recibido_en",insertable=false,updatable=false) private LocalDateTime recibidoEn;
    @Column(name="procesado_en") private LocalDateTime procesadoEn;
}
