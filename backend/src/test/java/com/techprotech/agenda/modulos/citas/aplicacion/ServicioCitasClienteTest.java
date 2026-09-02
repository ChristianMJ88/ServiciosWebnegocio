package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioCitasClienteTest {

    @Mock private ClienteRepositorio clienteRepositorio;
    @InjectMocks private ServicioCitas servicioCitas;

    @Test
    void creaPerfilClienteCuandoElCorreoYaPerteneceAUnUsuarioInterno() {
        when(clienteRepositorio.findById(42L)).thenReturn(Optional.empty());

        Long clienteId = servicioCitas.actualizarClienteExistente(42L, "  Christian Mejia Lara  ", " 2213688033 ");

        ArgumentCaptor<ClienteEntidad> clienteCaptor = ArgumentCaptor.forClass(ClienteEntidad.class);
        verify(clienteRepositorio).save(clienteCaptor.capture());
        ClienteEntidad cliente = clienteCaptor.getValue();
        assertThat(clienteId).isEqualTo(42L);
        assertThat(cliente.getUsuarioId()).isEqualTo(42L);
        assertThat(cliente.getNombreCompleto()).isEqualTo("Christian Mejia Lara");
        assertThat(cliente.getTelefono()).isEqualTo("2213688033");
        assertThat(cliente.isAceptaWhatsapp()).isTrue();
    }
}
