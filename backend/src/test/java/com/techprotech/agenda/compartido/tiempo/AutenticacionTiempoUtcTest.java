package com.techprotech.agenda.compartido.tiempo;

import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioProteccionAcceso;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRecuperacionContrasena;
import com.techprotech.agenda.modulos.autenticacion.api.ConfirmarRecuperacionRequest;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.*;
import com.techprotech.agenda.seguridad.jwt.PropiedadesJwt;
import com.techprotech.agenda.seguridad.jwt.ServicioTokenJwt;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Isolated("La seguridad usa instantes UTC con JVM UTC o México")
class AutenticacionTiempoUtcTest {
    final Clock reloj=Clock.fixed(Instant.parse("2026-10-15T14:00:00Z"),ZoneOffset.UTC);

    @ParameterizedTest @ValueSource(strings={"UTC","America/Mexico_City"})
    void emisionYValidacionJwtCompartenElRelojYRespetanElVencimiento(String zona) {
        var anterior=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone(zona));
        try {
            var propiedades=new PropiedadesJwt("secreto-exclusivo-de-pruebas-sin-valor-en-produccion", "pruebas",15,7);
            var tokens=new ServicioTokenJwt(propiedades,reloj);
            var token=tokens.generarTokenAcceso("usuario",9L,7L,"negocio",List.of(),List.of(),List.of());
            assertThat(tokens.esValido(token)).isTrue();assertThat(tokens.obtenerEmpresaId(token)).isEqualTo(7L);
            assertThat(new ServicioTokenJwt(propiedades,Clock.offset(reloj,Duration.ofMinutes(14))).esValido(token)).isTrue();
            assertThat(new ServicioTokenJwt(propiedades,Clock.offset(reloj,Duration.ofMinutes(16))).esValido(token)).isFalse();
        } finally {TimeZone.setDefault(anterior);}
    }

    @ParameterizedTest @ValueSource(strings={"UTC","America/Mexico_City"})
    void bloqueoTemporalSeEscribeYSeEvaluaEnUtc(String zona) {
        var anterior=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone(zona));
        try {
            var usuarios=mock(UsuarioRepositorio.class);
            var usuario=new UsuarioEntidad();usuario.setId(9L);usuario.setIntentosLoginFallidos(2);
            when(usuarios.findById(9L)).thenReturn(Optional.of(usuario));
            var servicio=new ServicioProteccionAcceso(usuarios,3,10,reloj);
            servicio.registrarIntentoFallido(9L);
            assertThat(usuario.getLoginBloqueadoHasta()).isEqualTo(LocalDateTime.parse("2026-10-15T14:10"));
            assertThat(servicio.estaBloqueadoTemporalmente(usuario)).isTrue();
            assertThat(new ServicioProteccionAcceso(usuarios,3,10,Clock.offset(reloj,Duration.ofMinutes(11)))
                    .estaBloqueadoTemporalmente(usuario)).isFalse();
        } finally {TimeZone.setDefault(anterior);}
    }

    @ParameterizedTest @ValueSource(strings={"UTC","America/Mexico_City"})
    void recuperacionValidaUtcYRechazaElEnlaceTrasVencer(String zona) {
        var anterior=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone(zona));
        try {
            var recuperaciones=mock(RecuperacionContrasenaRepositorio.class);var usuarios=mock(UsuarioRepositorio.class);
            var encoder=mock(PasswordEncoder.class);
            var recuperacion=new RecuperacionContrasenaEntidad();recuperacion.setId(1L);recuperacion.setUsuarioId(9L);
            recuperacion.setEmpresaId(7L);recuperacion.setEstado("PENDIENTE");
            recuperacion.setExpiraEn(LocalDateTime.parse("2026-10-15T14:10"));
            when(recuperaciones.findByTokenHash(anyString())).thenReturn(Optional.of(recuperacion));
            var vencido=new ServicioRecuperacionContrasena(usuarios,null,recuperaciones,null,null,encoder,null,"",10,
                    Clock.offset(reloj,Duration.ofMinutes(11)));
            var request=new ConfirmarRecuperacionRequest("token-de-prueba","Temporal123!");
            assertThatThrownBy(() -> vencido.confirmar(request)).isInstanceOfSatisfying(ResponseStatusException.class,
                    ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.GONE));
            verifyNoInteractions(usuarios,encoder);
            var usuario=new UsuarioEntidad();usuario.setId(9L);usuario.setEmpresaId(7L);usuario.setHabilitado(true);
            when(usuarios.findByIdAndEmpresaId(9L,7L)).thenReturn(Optional.of(usuario));
            when(encoder.encode(anyString())).thenReturn("hash-de-prueba");
            var vigente=new ServicioRecuperacionContrasena(usuarios,null,recuperaciones,null,null,encoder,null,"",10,reloj);
            vigente.confirmar(request);
            assertThat(recuperacion.getUtilizadaEn()).isEqualTo(LocalDateTime.parse("2026-10-15T14:00"));
            assertThat(recuperacion.getEstado()).isEqualTo("UTILIZADA");
        } finally {TimeZone.setDefault(anterior);}
    }
}
