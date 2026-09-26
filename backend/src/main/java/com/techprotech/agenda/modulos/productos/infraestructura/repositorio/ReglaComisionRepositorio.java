package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ReglaComisionEntidad; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ReglaComisionRepositorio extends JpaRepository<ReglaComisionEntidad,Long>{List<ReglaComisionEntidad> findByEmpresaIdOrderByPrioridadDescNombreAsc(Long empresaId);Optional<ReglaComisionEntidad> findByIdAndEmpresaId(Long id,Long empresaId);}
