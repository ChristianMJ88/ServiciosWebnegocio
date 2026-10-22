package com.techprotech.agenda.modulos.dominios.infraestructura.repositorio;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.LocalDateTime;
public interface EmpresaDominioRepositorio extends JpaRepository<EmpresaDominioEntidad,Long> {
 List<EmpresaDominioEntidad> findByEmpresaIdOrderByIdDesc(Long empresaId);
 Optional<EmpresaDominioEntidad> findByIdAndEmpresaId(Long id,Long empresaId);
 long countByEmpresaIdAndEstadoNot(Long empresaId,EstadoDominio estado);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from EmpresaDominioEntidad d where d.hostname=:hostname order by d.id")
 List<EmpresaDominioEntidad> bloquearHostname(String hostname);
 @Query("select max(d.ultimoChequeoEn) from EmpresaDominioEntidad d where d.empresaId=:empresaId")
 LocalDateTime ultimoChequeoEmpresa(Long empresaId);
 Optional<EmpresaDominioEntidad> findByHostnameAndEstado(String hostname,EstadoDominio estado);
 Page<EmpresaDominioEntidad> findByEstado(EstadoDominio estado,Pageable pageable);
 @Query("select d.id from EmpresaDominioEntidad d where d.estado in :estados and (d.proximoChequeoEn is null or d.proximoChequeoEn<=:ahora) order by d.proximoChequeoEn asc,d.id asc")
 List<Long> candidatos(List<EstadoDominio> estados,LocalDateTime ahora,Pageable pageable);
 @Modifying @Query("update EmpresaDominioEntidad d set d.proximoChequeoEn=:proximo,d.intentosChequeo=d.intentosChequeo+1 where d.id=:id")
 int planificar(Long id,LocalDateTime proximo);
 @Modifying @Query("update EmpresaDominioEntidad d set d.intentosChequeo=case when d.intentosChequeo>0 then d.intentosChequeo-1 else 0 end where d.id=:id")
 int descontarIntento(Long id);
}
