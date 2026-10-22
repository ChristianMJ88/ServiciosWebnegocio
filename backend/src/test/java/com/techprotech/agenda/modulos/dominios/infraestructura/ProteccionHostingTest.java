package com.techprotech.agenda.modulos.dominios.infraestructura;
import com.techprotech.agenda.modulos.dominios.infraestructura.red.ComprobadorHostingHttps;
import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetAddress;
import static org.assertj.core.api.Assertions.*;
class ProteccionHostingTest {
 @ParameterizedTest @ValueSource(strings={"127.0.0.1","0.0.0.0","10.0.0.1","172.16.0.1","192.168.1.1","169.254.169.254","100.64.0.1","198.18.0.1","224.0.0.1","::1","::","fc00::1","fe80::1","::ffff:127.0.0.1","64:ff9b::a00:1","2002:7f00:1::1","2001:0::1","192.88.99.1"})
 void rechazaRedesInternasSinHacerLlamadas(String ip)throws Exception{assertThat(ComprobadorHostingHttps.esPublica(InetAddress.getByName(ip))).isFalse();}
 @ParameterizedTest @ValueSource(strings={"8.8.8.8","1.1.1.1","2606:4700:4700::1111"})
 void reconoceDireccionGlobal(String ip)throws Exception{assertThat(ComprobadorHostingHttps.esPublica(InetAddress.getByName(ip))).isTrue();}
}
