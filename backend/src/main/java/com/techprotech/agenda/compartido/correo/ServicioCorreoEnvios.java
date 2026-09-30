package com.techprotech.agenda.compartido.correo;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioPedidoEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EnvioPedidoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioCorreoEnvios {
    private final ServicioConfiguracionCorreoEmpresa configuraciones;
    private final ClienteCorreoSaliente cliente;
    private final EnvioPedidoRepositorio envios;
    private final CheckoutOrdenRepositorio ordenes;
    private final EmpresaSitioConfigRepositorio sitios;

    public ServicioCorreoEnvios(ServicioConfiguracionCorreoEmpresa configuraciones, ClienteCorreoSaliente cliente,
                                EnvioPedidoRepositorio envios, CheckoutOrdenRepositorio ordenes,
                                EmpresaSitioConfigRepositorio sitios) {
        this.configuraciones = configuraciones; this.cliente = cliente; this.envios = envios;
        this.ordenes = ordenes; this.sitios = sitios;
    }

    public void enviar(Long empresaId, String tipoEvento, EventoEnvioCorreoPayload payload) {
        ConfiguracionCorreoResolvida configuracion = configuraciones.resolver(empresaId);
        if (!configuracion.habilitado()) throw new IllegalStateException("El correo del negocio no está habilitado");
        EnvioPedidoEntidad envio = envios.findById(payload.envioId())
                .filter(item -> empresaId.equals(item.getEmpresaId())).orElseThrow();
        CheckoutOrdenEntidad orden = ordenes.findById(envio.getCheckoutOrdenId()).orElseThrow();
        String negocio = sitios.findById(empresaId).map(item -> item.getNombreComercial()).orElse("el negocio");
        Datos datos = datos(tipoEvento);
        String guia = texto(envio.getNumeroGuia()) ? envio.getNumeroGuia() : "Pendiente";
        String rastreo = texto(envio.getUrlRastreo()) ? envio.getUrlRastreo() : envio.getUrlEtiqueta();
        String plano = "Hola " + orden.getNombreDestinatario() + ",\n\n" + datos.mensaje + "\n\n"
                + "Orden: " + orden.getPublicId() + "\nGuía: " + guia
                + (texto(envio.getPaqueteria()) ? "\nPaquetería: " + envio.getPaqueteria() : "")
                + (texto(rastreo) ? "\nSeguimiento: " + rastreo : "") + "\n\n" + negocio;
        String boton = texto(rastreo) ? "<p style=\"margin:24px 0 0\"><a href=\"" + attr(rastreo)
                + "\" style=\"display:inline-block;padding:13px 20px;border-radius:12px;background:#6d28d9;color:#fff;text-decoration:none;font-weight:700\">Rastrear mi pedido</a></p>" : "";
        String html = "<!doctype html><html lang=\"es\"><body style=\"margin:0;background:#f4f6fb;font-family:Arial,sans-serif;color:#172033\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding:24px 12px\"><tr><td align=\"center\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:620px;background:#fff;border-radius:22px;overflow:hidden\"><tr><td style=\"padding:30px 36px;background:#312e81;color:#fff\"><small style=\"letter-spacing:.12em;text-transform:uppercase\">Actualización de envío</small><h1 style=\"margin:10px 0 0\">" + esc(datos.titulo) + "</h1></td></tr><tr><td style=\"padding:30px 36px\"><p style=\"font-size:16px;line-height:1.6\">Hola " + esc(orden.getNombreDestinatario()) + ", " + esc(datos.mensaje) + "</p><div style=\"padding:18px;border-radius:14px;background:#f7f8fc\"><strong>Guía:</strong> " + esc(guia) + "<br><strong>Orden:</strong> " + esc(orden.getPublicId()) + (texto(envio.getPaqueteria()) ? "<br><strong>Paquetería:</strong> " + esc(envio.getPaqueteria()) : "") + "</div>" + boton + "<p style=\"margin-top:26px;color:#64748b;font-size:14px\">" + esc(negocio) + "</p></td></tr></table></td></tr></table></body></html>";
        cliente.enviar(configuracion, new MensajeCorreoSaliente(orden.getCorreoCliente(),
                datos.asunto + " | " + negocio, plano, html, configuracion.responderA(), List.of()));
    }

    private Datos datos(String tipo) {
        return switch (tipo) {
            case "ENVIO_GUIA_GENERADA_EMAIL" -> new Datos("Tu pedido ya tiene guía", "Guía de envío creada", "tu pedido ya tiene una guía de envío.");
            case "ENVIO_EN_TRANSITO_EMAIL" -> new Datos("Tu pedido está en camino", "Pedido en tránsito", "tu pedido ya está en tránsito.");
            case "ENVIO_ENTREGADO_EMAIL" -> new Datos("Tu pedido fue entregado", "Pedido entregado", "la paquetería reportó tu pedido como entregado.");
            case "ENVIO_INCIDENCIA_EMAIL" -> new Datos("Actualización importante de tu envío", "Incidencia de envío", "la paquetería reportó una incidencia. El negocio dará seguimiento.");
            default -> throw new IllegalArgumentException("Evento de envío no soportado: " + tipo);
        };
    }

    private boolean texto(String valor) { return valor != null && !valor.isBlank(); }
    private String esc(String valor) { return valor == null ? "" : valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    private String attr(String valor) { return esc(valor); }
    private record Datos(String asunto, String titulo, String mensaje) {}
}
