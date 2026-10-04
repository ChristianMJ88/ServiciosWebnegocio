package com.techprotech.agenda.modulos.plataforma.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name="plataforma_stripe_cliente")
public class StripeClientePlataformaEntidad {
    @Id @Column(name="empresa_id") private Long empresaId;
    @Column(name="stripe_cliente_id",nullable=false,unique=true,length=100) private String stripeClienteId;
    @Column(name="creado_en",insertable=false,updatable=false) private LocalDateTime creadoEn;
    @Column(name="actualizado_en",insertable=false,updatable=false) private LocalDateTime actualizadoEn;
}
