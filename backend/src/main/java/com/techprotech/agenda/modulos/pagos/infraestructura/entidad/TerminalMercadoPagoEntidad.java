package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name = "mercado_pago_terminal")
public class TerminalMercadoPagoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "sucursal_id") private Long sucursalId;
    @Column(name = "terminal_id", nullable = false, length = 180) private String terminalId;
    @Column(name = "pos_id", length = 180) private String posId;
    @Column(name = "store_id", length = 180) private String storeId;
    @Column(name = "external_pos_id", length = 180) private String externalPosId;
    @Column(nullable = false, length = 160) private String nombre;
    @Column(name = "modo_operacion", length = 30) private String modoOperacion;
    @Column(nullable = false) private boolean activa = true;
    @Column(name = "creado_en", insertable = false, updatable = false) private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false) private LocalDateTime actualizadoEn;
}
