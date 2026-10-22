package com.techprotech.agenda.modulos.dominios.infraestructura.red;
import com.techprotech.agenda.modulos.dominios.aplicacion.ResolverTxtDominio;
import org.springframework.stereotype.Component;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import java.util.*;
import java.util.regex.Pattern;
@Component
public class ResolverTxtDns implements ResolverTxtDominio {
 public List<String> consultar(String hostname) throws Exception {
  var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
  var task=executor.submit(()->consultarInterno(hostname));
  try{return task.get(5,java.util.concurrent.TimeUnit.SECONDS);}finally{task.cancel(true);executor.shutdownNow();}
 }
 protected List<String> consultarInterno(String hostname) throws Exception {
  var env=new Hashtable<String,String>();env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.dns.DnsContextFactory");env.put("com.sun.jndi.dns.timeout.initial","1000");env.put("com.sun.jndi.dns.timeout.retries","1");
  var contexto=new InitialDirContext(env);
  try{var txt=contexto.getAttributes("dns:/"+hostname,new String[]{"TXT"}).get("TXT");if(txt==null)return List.of();var result=new ArrayList<String>();
   for(int i=0;i<Math.min(txt.size(),50);i++){String raw=String.valueOf(txt.get(i));if(raw.length()>4096)continue;var matcher=Pattern.compile("\"([^\"]*)\"").matcher(raw);var joined=new StringBuilder();boolean quoted=false;while(matcher.find()){quoted=true;joined.append(matcher.group(1));}result.add(quoted?joined.toString():raw);}
   return List.copyOf(result);
  }finally{contexto.close();}
 }
}
