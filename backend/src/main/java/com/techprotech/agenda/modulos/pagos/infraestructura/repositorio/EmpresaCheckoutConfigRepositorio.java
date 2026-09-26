package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EmpresaCheckoutConfigEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaCheckoutConfigRepositorio extends JpaRepository<EmpresaCheckoutConfigEntidad, Long> {
}
