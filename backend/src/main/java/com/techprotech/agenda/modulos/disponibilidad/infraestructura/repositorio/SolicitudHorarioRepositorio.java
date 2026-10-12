package com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.SolicitudHorarioEntidad;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface SolicitudHorarioRepositorio extends JpaRepository<SolicitudHorarioEntidad,Long> {
 List<SolicitudHorarioEntidad> findByEmpresaIdAndEstadoOrderByCreadaEnAsc(Long empresaId,String estado);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 Optional<SolicitudHorarioEntidad> findByIdAndEmpresaId(Long id,Long empresaId);
}
