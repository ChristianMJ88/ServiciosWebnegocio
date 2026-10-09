package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SuscripcionPlataformaRepositorio extends JpaRepository<SuscripcionPlataformaEntidad,Long>{
    @org.springframework.data.jpa.repository.Query("""
            select s from SuscripcionPlataformaEntidad s where s.correoFacturacion is not null
            and s.avisoVencimientoEn is null and s.estado in ('ACTIVA','PRUEBA','GRACIA')
            and ((s.estado='PRUEBA' and s.pruebaHasta<=:ahora)
            or (s.estado='GRACIA' and s.graciaHasta<=:ahora)
            or (s.cancelarAlFinalPeriodo=true and s.periodoActualHasta<=:ahora)
            or (s.estado='ACTIVA' and s.stripeSuscripcionId is not null and s.periodoActualHasta<=:ahora)
            or s.terminaEn<=:ahora) order by s.id
            """)
    List<SuscripcionPlataformaEntidad> findVencidasSinAvisar(@org.springframework.data.repository.query.Param("ahora") java.time.LocalDateTime ahora,org.springframework.data.domain.Pageable pagina);
    @org.springframework.data.jpa.repository.Query("""
            select s from SuscripcionPlataformaEntidad s where s.stripeSuscripcionId is not null and s.id>:despuesId
            and ((s.estado='ACTIVA' and (s.periodoActualHasta is null or s.periodoActualHasta<=:ahora))
            or s.estado in ('GRACIA','SUSPENDIDA')) order by s.id
            """)
    List<SuscripcionPlataformaEntidad> findParaConciliar(@org.springframework.data.repository.query.Param("ahora") java.time.LocalDateTime ahora,@org.springframework.data.repository.query.Param("despuesId") Long despuesId,org.springframework.data.domain.Pageable pagina);
    List<SuscripcionPlataformaEntidad> findAllByOrderByCreadaEnDesc();
    List<SuscripcionPlataformaEntidad> findByEmpresaIdAndEstadoIn(Long empresaId, List<String> estados);
    List<SuscripcionPlataformaEntidad> findByEmpresaIdAndEstadoInOrderByCreadaEnDesc(Long empresaId, List<String> estados);
    Optional<SuscripcionPlataformaEntidad> findByStripeCheckoutSesionId(String stripeCheckoutSesionId);
    Optional<SuscripcionPlataformaEntidad> findByStripeSuscripcionId(String stripeSuscripcionId);
    boolean existsByEmpresaIdAndPruebaHastaIsNotNull(Long empresaId);
    Optional<SuscripcionPlataformaEntidad> findFirstByEmpresaIdOrderByCreadaEnDesc(Long empresaId);
}
