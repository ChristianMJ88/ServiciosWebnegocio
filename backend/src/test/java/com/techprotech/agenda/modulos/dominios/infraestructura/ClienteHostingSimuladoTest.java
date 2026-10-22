package com.techprotech.agenda.modulos.dominios.infraestructura;
import com.techprotech.agenda.modulos.dominios.infraestructura.red.ComprobadorHostingHttps;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import static org.assertj.core.api.Assertions.*;
class ClienteHostingSimuladoTest {
 private ComprobadorHostingHttps fake(int status,String body,boolean delay){return new ComprobadorHostingHttps(){
  @Override protected CloseableHttpClient crearCliente(String host){return new CloseableHttpClient(){
   protected CloseableHttpResponse doExecute(HttpHost target,ClassicHttpRequest request,HttpContext context)throws IOException {
    assertThat(target.getSchemeName()).isEqualTo("https");assertThat(target.getHostName()).isEqualTo("marca.mx");assertThat(request.getPath()).isEqualTo("/fluora-host.txt");
    if(delay)try{Thread.sleep(6000);}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IOException("cancelled");}
    var response=new BasicClassicHttpResponse(status);response.setEntity(new StringEntity(body,ContentType.TEXT_PLAIN));if(status==302)response.setHeader("Location","https://other.example/fluora-host.txt");return CloseableHttpResponse.adapt(response);
   }
   public void close(){}public void close(CloseMode mode){}
  };}
 };}
 @Test void aceptaMarcaCorrectaSinRedExterna()throws Exception{assertThat(fake(200,"fluora-host:fixture-clientes:"+"a".repeat(64)+"\n",false).comprobar("marca.mx","fixture-clientes")).isTrue();}
 @Test void noAceptaRedirectNiSigueLocation()throws Exception{assertThat(fake(302,"redirect",false).comprobar("marca.mx","fixture-clientes")).isFalse();}
 @Test void limitaTamanoYRechazaMarcaAjena()throws Exception{assertThat(fake(200,"x".repeat(1025),false).comprobar("marca.mx","fixture-clientes")).isFalse();assertThat(fake(200,"fluora-host:otro-sitio:"+"b".repeat(64)+"\n",false).comprobar("marca.mx","fixture-clientes")).isFalse();}
 @Test void cancelaAlLlegarAlLimiteTotal(){long start=System.nanoTime();assertThatThrownBy(()->fake(200,"",true).comprobar("marca.mx","fixture-clientes")).isInstanceOf(TimeoutException.class);assertThat((System.nanoTime()-start)/1_000_000).isBetween(4900L,5700L);}
}
