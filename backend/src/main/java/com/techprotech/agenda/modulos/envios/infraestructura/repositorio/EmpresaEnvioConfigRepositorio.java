package com.techprotech.agenda.modulos.envios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EmpresaEnvioConfigEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaEnvioConfigRepositorio extends JpaRepository<EmpresaEnvioConfigEntidad, Long> {}
