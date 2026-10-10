package com.techprotech.agenda.modulos.marketplace.infraestructura.repositorio;
import com.techprotech.agenda.modulos.marketplace.infraestructura.entidad.ParticipacionMarketplaceEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ParticipacionMarketplaceRepositorio extends JpaRepository<ParticipacionMarketplaceEntidad,Long> {}
