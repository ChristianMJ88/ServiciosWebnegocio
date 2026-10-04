package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.StripeEventoPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StripeEventoPlataformaRepositorio extends JpaRepository<StripeEventoPlataformaEntidad,Long> {
    boolean existsByStripeEventoId(String stripeEventoId);
}
