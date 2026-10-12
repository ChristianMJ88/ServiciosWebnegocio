package com.techprotech.agenda.modulos.disponibilidad.aplicacion;
import java.util.List;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.*;
public interface ServicioHorarioSemanal {
 List<ReglaDisponibilidadResponse> guardar(Long empresaId,HorarioSemanalRequest request);
 List<ReglaDisponibilidadResponse> guardarStaff(Long empresaId,Long usuarioId,HorarioSemanalRequest request);
}
