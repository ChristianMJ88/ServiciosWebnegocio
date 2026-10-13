package com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="empresa_configuracion_reservas") @Getter @Setter
public class EmpresaConfiguracionReservasEntidad {
    @Column(name="horas_tope_procesamiento_pago") private Integer horasTopeProcesamientoPago;
    @Id @Column(name="empresa_id") private Long empresaId;
    @Column(name="anticipacion_minima_minutos",nullable=false) private int anticipacionMinimaMinutos;
    @Column(name="ventana_reserva_dias",nullable=false) private int ventanaReservaDias;
    @Column(name="liberacion_sin_confirmacion_horas",nullable=false) private int liberacionSinConfirmacionHoras;
    @Column(name="minutos_apartado_pago",nullable=false) private int minutosApartadoPago;
}
