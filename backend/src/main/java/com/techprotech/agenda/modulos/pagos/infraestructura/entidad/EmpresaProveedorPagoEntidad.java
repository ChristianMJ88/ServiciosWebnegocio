package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "empresa_proveedor_pago")
public class EmpresaProveedorPagoEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "proveedor_pago_id", nullable = false)
    private Long proveedorPagoId;
    @Column(nullable = false, length = 30)
    private String estado;
    @Column(nullable = false, length = 20)
    private String ambiente;
    @Column(name = "cuenta_externa_id", length = 160)
    private String cuentaExternaId;
    @Column(name = "correo_cuenta", length = 180)
    private String correoCuenta;
    @Column(name = "llave_publica", length = 500)
    private String llavePublica;
    @Column(name = "token_acceso_cifrado", columnDefinition = "MEDIUMTEXT")
    private String tokenAccesoCifrado;
    @Column(name = "token_actualizacion_cifrado", columnDefinition = "MEDIUMTEXT")
    private String tokenActualizacionCifrado;
    @Column(name = "token_expira_en")
    private LocalDateTime tokenExpiraEn;
    @Column(name = "conectado_en")
    private LocalDateTime conectadoEn;
    @Column(name = "desconectado_en")
    private LocalDateTime desconectadoEn;
    @Column(name = "ultima_sincronizacion_en")
    private LocalDateTime ultimaSincronizacionEn;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
