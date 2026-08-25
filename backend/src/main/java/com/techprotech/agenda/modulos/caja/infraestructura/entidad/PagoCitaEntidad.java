package com.techprotech.agenda.modulos.caja.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Setter
@Getter
@Entity
@Table(name = "pago_cita")
public class PagoCitaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Column(name = "caja_sesion_id")
    private Long cajaSesionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo_pago", nullable = false, length = 30)
    private String metodoPago;

    @Column(length = 120)
    private String referencia;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "registrado_por_usuario_id", nullable = false)
    private Long registradoPorUsuarioId;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

}
