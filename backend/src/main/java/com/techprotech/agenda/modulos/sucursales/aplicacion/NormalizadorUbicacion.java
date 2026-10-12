package com.techprotech.agenda.modulos.sucursales.aplicacion;
import java.text.Normalizer;
public final class NormalizadorUbicacion {
 private NormalizadorUbicacion(){}
 public static String texto(String valor){
  if(valor==null)return null;
  var limpio=Normalizer.normalize(valor,Normalizer.Form.NFC).replaceAll("(?U)\\s+"," ").strip();
  return limpio.isEmpty()?null:limpio;
 }
}
