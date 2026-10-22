package com.techprotech.agenda.modulos.dominios.aplicacion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ValidadorHostnameTest {
 private final ValidadorHostname validador=new ValidadorHostname("https://platform.example","https://console.example");
 @ParameterizedTest @ValueSource(strings={"platform.example","www.platform.example","a.platform.example","console.example","a.console.example","customer.web.app","customer.firebaseapp.com","localhost","x.localhost","127.0.0.1","192.168.1.1","[::1]","https://brand.mx","brand.mx:443","brand.mx/path","brand.mx?query","brand.mx#fragment","*.brand.mx","com.mx","co.uk","com","a..mx"," brand.mx","brand.mx ","brand.mx.","brand@mx.com"})
 void rechazaEntradasNoPermitidas(String host){assertThatThrownBy(()->validador.normalizar(host)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}
 @Test void raicesSubdominiosYAscii(){assertThat(validador.normalizar("MARCA.mx")).isEqualTo("marca.mx");assertThat(validador.normalizar("reservas.marca.mx")).isEqualTo("reservas.marca.mx");assertThat(validador.normalizar("uñas.mx")).isEqualTo("xn--uas-7ma.mx");assertThat(validador.esRaiz("marca.mx")).isTrue();assertThat(validador.esRaiz("www.marca.mx")).isFalse();}
 @Test void rechazaLongitud(){assertThatThrownBy(()->validador.normalizar("a".repeat(64)+".mx")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);assertThatThrownBy(()->validador.normalizar("a".repeat(254))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}
}
