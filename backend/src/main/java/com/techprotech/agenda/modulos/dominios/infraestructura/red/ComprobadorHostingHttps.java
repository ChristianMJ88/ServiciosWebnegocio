package com.techprotech.agenda.modulos.dominios.infraestructura.red;
import com.techprotech.agenda.modulos.dominios.aplicacion.ComprobadorHostingDominio;
import org.springframework.stereotype.Component;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.config.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.util.Timeout;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.*;
@Component
public class ComprobadorHostingHttps implements ComprobadorHostingDominio {
 public boolean comprobar(String hostname,String sitio) throws Exception {
  if(sitio==null||!sitio.matches("[a-z0-9][a-z0-9-]{0,62}")||!hostname.matches("[a-z0-9.-]+"))return false;
  var request=new HttpGet(new URI("https",hostname,"/fluora-host.txt",null));
  var executor=Executors.newVirtualThreadPerTaskExecutor();
  var future=executor.submit(()->ejecutar(hostname,sitio,request));
  try{return future.get(5,TimeUnit.SECONDS);}finally{request.cancel();future.cancel(true);executor.shutdownNow();}
 }
 protected org.apache.hc.client5.http.impl.classic.CloseableHttpClient crearCliente(String host) {
  DnsResolver dns=new DnsResolver(){public InetAddress[] resolve(String hostname) throws UnknownHostException{
   if(!host.equals(hostname))throw new UnknownHostException("Host inesperado");
   var ips=InetAddress.getAllByName(hostname);if(ips.length==0)throw new UnknownHostException("Sin dirección pública");
   for(var ip:ips)if(!esPublica(ip))throw new UnknownHostException("Dirección no pública");return ips;
  }public String resolveCanonicalHostname(String hostname){return hostname;}};
  // Validation and connection use the same DNS answer; no second unchecked resolution.
  var pool=PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(dns).setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(5)).setSocketTimeout(Timeout.ofSeconds(5)).build()).build();
  return HttpClients.custom().setConnectionManager(pool).disableRedirectHandling().disableAutomaticRetries().disableContentCompression().setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofSeconds(5)).setConnectionRequestTimeout(Timeout.ofSeconds(5)).build()).build();
 }
 private boolean ejecutar(String host,String sitio,HttpGet request) throws Exception {
  try(var client=crearCliente(host)){
   request.setHeader("Accept","text/plain");
   try(var response=client.execute(request)){
    try{
     if(response.getCode()!=200||response.getEntity()==null)return false;
     byte[] body=response.getEntity().getContent().readNBytes(1025);
     return body.length<=1024&&new String(body,StandardCharsets.UTF_8).matches("fluora-host:"+java.util.regex.Pattern.quote(sitio)+":[a-f0-9]{64}\\n?");
    }finally{request.cancel();} // Abort before closing the response; never drain an oversized entity.
   }
  }
 }
 public static boolean esPublica(InetAddress ip){
  if(ip.isAnyLocalAddress()||ip.isLoopbackAddress()||ip.isLinkLocalAddress()||ip.isSiteLocalAddress()||ip.isMulticastAddress())return false;
  byte[] b=ip.getAddress();int a=b[0]&255;
  if(b.length==4){int c=b[1]&255;return a!=0&&a!=127&&a<224&&!(a==100&&c>=64&&c<=127)&&!(a==192&&(c==0||c==88&&(b[2]&255)==99))&&!(a==198&&(c==18||c==19));}
  // Only global unicast; excludes ULA, mapped/compatible IPv4, NAT64 and link-local.
  return (a&0xe0)==0x20&&!(a==0x20&&(b[1]&255)==2)&&!(a==0x20&&(b[1]&255)==1&&b[2]==0&&b[3]==0)&&!(a==0x20&&(b[1]&255)==1&&(b[2]&255)==0x0d&&(b[3]&255)==0xb8);
 }
}
