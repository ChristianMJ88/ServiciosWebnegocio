package com.techprotech.agenda.modulos.marketplace.infraestructura.entidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="empresa_marketplace") @Getter @Setter
public class ParticipacionMarketplaceEntidad {
 @Id @Column(name="empresa_id") private Long empresaId;
 @Column(nullable=false) private boolean participante;
}
