package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioOAuthMercadoPago;

import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class PointTimeoutHttpTest {
    @Test void timeoutDeLecturaConfiguradoInterrumpeRespuestaLenta() throws Exception {
        var servidor=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var liberar=new java.util.concurrent.CountDownLatch(1);
        servidor.createContext("/lento", exchange -> {
            try {liberar.await(2,java.util.concurrent.TimeUnit.SECONDS);exchange.sendResponseHeaders(200,2);exchange.getResponseBody().write("{}".getBytes());}
            catch(InterruptedException e){Thread.currentThread().interrupt();}finally{exchange.close();}
        });
        servidor.start();
        try {
            var oauth=mock(ServicioOAuthMercadoPago.class);when(oauth.obtenerAccessToken(7L)).thenReturn("TOKEN_FICTICIO");
            var cliente=new ClienteMercadoPagoPoint(oauth,RestClient.builder(),mock(MapeadorRespuestaMercadoPago.class),500,50);
            long inicio=System.nanoTime();
            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(cliente,"get","http://127.0.0.1:"+servidor.getAddress().getPort()+"/lento",7L))
                .isInstanceOf(org.springframework.web.client.ResourceAccessException.class);
            assertThat(java.time.Duration.ofNanos(System.nanoTime()-inicio)).isLessThan(java.time.Duration.ofSeconds(2));
        } finally {liberar.countDown();servidor.stop(0);}
    }
}
