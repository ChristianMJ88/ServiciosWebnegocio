package com.techprotech.agenda.modulos.whatsapp.onboarding.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "whatsapp_onboarding")
public class WhatsappOnboardingEntidad {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, unique = true)
    private Long empresaId;

    @Column(name = "usuario_actor_id")
    private Long usuarioActorId;

    @Column(nullable = false, length = 40)
    private String estado;

    @Column(name = "paso_actual", length = 80)
    private String pasoActual;

    @Column(name = "telefono_e164", length = 40)
    private String telefonoE164;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "waba_id", length = 100)
    private String wabaId;

    @Column(name = "phone_number_id", length = 100)
    private String phoneNumberId;

    @Column(name = "channel_sender_sid", length = 80)
    private String channelSenderSid;

    @Column(name = "sender_asociado", nullable = false)
    private boolean senderAsociado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", length = 1000)
    private String ultimoError;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
