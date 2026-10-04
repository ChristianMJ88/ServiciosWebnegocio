package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.StripeClientePlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface StripeClientePlataformaRepositorio extends JpaRepository<StripeClientePlataformaEntidad,Long> {
    Optional<StripeClientePlataformaEntidad> findByStripeClienteId(String stripeClienteId);
}
