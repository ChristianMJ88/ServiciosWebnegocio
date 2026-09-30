package com.techprotech.agenda.modulos.envios.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "empresa_envio_config")
public class EmpresaEnvioConfigEntidad {
    @Id @Column(name = "empresa_id") private Long empresaId;
    @Column(nullable = false, length = 40) private String proveedor = "ENVIA";
    @Column(nullable = false) private boolean habilitado;
    @Column(nullable = false, length = 20) private String ambiente = "PRUEBA";
    @Column(name = "modo_credencial", nullable = false, length = 30) private String modoCredencial = "CUENTA_PROPIA";
    @Column(name = "token_cifrado", columnDefinition = "MEDIUMTEXT") private String tokenCifrado;
    @Column(name = "webhook_token_hash", length = 64, unique = true) private String webhookTokenHash;
    @Column(name = "webhook_token_cifrado", columnDefinition = "TEXT") private String webhookTokenCifrado;
    @Column(name = "generar_guia_automaticamente", nullable = false) private boolean generarGuiaAutomaticamente;
    @Column(name = "permitir_seleccion_paquetera", nullable = false) private boolean permitirSeleccionPaquetera = true;
    @Column(name = "tipo_recargo", nullable = false, length = 20) private String tipoRecargo = "NINGUNO";
    @Column(name = "valor_recargo", nullable = false, precision = 12, scale = 4) private BigDecimal valorRecargo = BigDecimal.ZERO;
    @Column(name = "actualizado_en", insertable = false, updatable = false) private LocalDateTime actualizadoEn;
}
