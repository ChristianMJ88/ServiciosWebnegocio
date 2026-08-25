package com.techprotech.agenda.modulos.whatsapp.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(
        name = "whatsapp_conversacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_whatsapp_conversacion_empresa_telefono", columnNames = {"empresa_id", "telefono_normalizado"})
)
public class ConversacionWhatsappEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "telefono_normalizado", nullable = false, length = 30)
    private String telefonoNormalizado;

    @Column(name = "flujo", nullable = false, length = 40)
    private String flujo;

    @Column(name = "paso", nullable = false, length = 40)
    private String paso;

    @Column(name = "sucursal_id")
    private Long sucursalId;

    @Column(name = "grupo_id")
    private Long grupoId;

    @Column(name = "subgrupo_id")
    private Long subgrupoId;

    @Column(name = "servicio_id")
    private Long servicioId;

    @Column(name = "cita_id")
    private Long citaId;

    @Column(name = "fecha_seleccionada")
    private LocalDate fechaSeleccionada;

    @Column(name = "hora_seleccionada", length = 10)
    private String horaSeleccionada;

    @Column(name = "nombre_cliente", length = 150)
    private String nombreCliente;

    @Column(name = "correo_cliente", length = 150)
    private String correoCliente;

    @Column(name = "actualizada_en", nullable = false)
    private LocalDateTime actualizadaEn;

}
