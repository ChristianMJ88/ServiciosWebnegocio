package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "empresa_metodo_pago_checkout")
public class EmpresaMetodoPagoCheckoutEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "proveedor_pago_id", nullable = false)
    private Long proveedorPagoId;
    @Column(name = "metodo_externo_id", nullable = false, length = 80)
    private String metodoExternoId;
    @Column(name = "tipo_externo", nullable = false, length = 40)
    private String tipoExterno;
    @Column(nullable = false, length = 160)
    private String nombre;
    @Column(nullable = false)
    private boolean activo;
    @Column(name = "solo_productos", nullable = false)
    private boolean soloProductos = true;
    @Column(name = "vigencia_dias", nullable = false)
    private int vigenciaDias = 3;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
