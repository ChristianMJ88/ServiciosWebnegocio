package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceUpdateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.billingportal.SessionCreateParams.Builder;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.plataforma.api.dto.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.stripe.PropiedadesStripePlataforma;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ServicioStripePlataforma {
    private final java.time.Clock reloj;
    private static final List<String> ESTADOS_CON_ACCESO=List.of("ACTIVA","PRUEBA","GRACIA");
    private final PropiedadesStripePlataforma propiedades;
    private final PlanPlataformaRepositorio planes;
    private final SuscripcionPlataformaRepositorio suscripciones;
    private final StripeClientePlataformaRepositorio clientes;
    private final StripeEventoPlataformaRepositorio eventos;
    private final EmpresaRepositorio empresas;
    private final UsuarioRepositorio usuarios;
    private final ServicioAuditoriaPlataforma auditoria;
    private final ObjectMapper objectMapper;
    private final ServicioPlanSuscripcionStripe planSuscripcion;
    private final ServicioAvisosSuscripcion avisos;

    public ServicioStripePlataforma(PropiedadesStripePlataforma propiedades, PlanPlataformaRepositorio planes,
            SuscripcionPlataformaRepositorio suscripciones, StripeClientePlataformaRepositorio clientes,
            StripeEventoPlataformaRepositorio eventos, EmpresaRepositorio empresas, UsuarioRepositorio usuarios,
            ServicioAuditoriaPlataforma auditoria, ObjectMapper objectMapper,ServicioPlanSuscripcionStripe planSuscripcion,ServicioAvisosSuscripcion avisos, java.time.Clock reloj) {
        this.reloj = reloj;
        this.propiedades=propiedades; this.planes=planes; this.suscripciones=suscripciones; this.clientes=clientes;
        this.eventos=eventos; this.empresas=empresas; this.usuarios=usuarios; this.auditoria=auditoria; this.objectMapper=objectMapper;
        this.planSuscripcion=planSuscripcion;
        this.avisos=avisos;
    }

    public EstadoStripePlataformaResponse estado() {
        String base=limpiarBase(propiedades.webhookPublicBaseUrl());
        return new EstadoStripePlataformaResponse(propiedades.habilitado(), ambiente(), presente(propiedades.secretKey()),
                presente(propiedades.webhookSecret()), base==null?null:base+"/api/v1/publico/plataforma/stripe/webhook");
    }

    @Transactional(readOnly=true)
    public EstadoSuscripcionEmpresaResponse estadoSuscripcion(Long empresaId) {
        return suscripciones.findFirstByEmpresaIdOrderByCreadaEnDesc(empresaId).map(suscripcion -> {
            String planNombre=planes.findById(suscripcion.getPlanId()).map(PlanPlataformaEntidad::getNombre).orElse(null);
            return new EstadoSuscripcionEmpresaResponse(true,suscripcion.getId(),suscripcion.getPlanId(),planNombre,
                    suscripcion.getEstado(),suscripcion.getCiclo(),suscripcion.getPrecioAcordado(),suscripcion.getMoneda(),
                    suscripcion.getIniciaEn(),"PRUEBA".equals(suscripcion.getEstado())?suscripcion.getPruebaHasta():null,suscripcion.getPeriodoActualHasta(),
                    suscripcion.isCancelarAlFinalPeriodo(),presente(suscripcion.getStripeClienteId()),puedeContratar(empresaId),!suscripciones.existsByEmpresaIdAndPruebaHastaIsNotNull(empresaId));
        }).orElseGet(EstadoSuscripcionEmpresaResponse::sinSuscripcion);
    }

    private boolean puedeContratar(Long empresaId) {
        var ahora=LocalDateTime.now(reloj);
        return suscripciones.findByEmpresaIdAndEstadoIn(empresaId,ESTADOS_CON_ACCESO).stream()
                .noneMatch(s->presente(s.getStripeSuscripcionId())||PoliticaVigenciaSuscripcion.estaVigente(s,ahora));
    }

    @Transactional
    public SincronizarPlanStripeResponse sincronizarPlan(Long usuarioId,Long planId) {
        validarConfiguracion(false);
        PlanPlataformaEntidad plan=planes.findById(planId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"El plan no existe"));
        String hash=hashPlan(plan);
        try {
            RequestOptions opciones=opciones("plan-"+plan.getId()+"-"+hash);
            String productoId=plan.getStripeProductoId();
            if(!presente(productoId)) {
                ProductCreateParams.Builder productoParams=ProductCreateParams.builder().setName(plan.getNombre())
                        .putMetadata("fluora_plan_id",String.valueOf(plan.getId()))
                        .putMetadata("fluora_plan_codigo",plan.getCodigo());
                if(presente(plan.getDescripcion()))productoParams.setDescription(plan.getDescripcion());
                Product producto=Product.create(productoParams.build(),opciones);
                productoId=producto.getId();
            } else {
                Product producto=Product.retrieve(productoId,opciones);
                ProductUpdateParams.Builder productoParams=ProductUpdateParams.builder().setName(plan.getNombre())
                        .setActive(plan.isActivo());
                if(presente(plan.getDescripcion()))productoParams.setDescription(plan.getDescripcion());
                producto.update(productoParams.build(),opciones);
            }
            plan.setStripeProductoId(productoId);
            boolean precioAnualListo=plan.getPrecioAnual()==null||presente(plan.getStripePrecioAnualId());
            if(hash.equals(plan.getStripeConfigHash())&&presente(plan.getStripePrecioMensualId())&&precioAnualListo) {
                planes.save(plan);
                return sincronizado(plan,false);
            }
            String precioMensualAnterior=plan.getStripePrecioMensualId();
            String precioAnualAnterior=plan.getStripePrecioAnualId();
            Price mensual=crearPrecio(plan,productoId,plan.getPrecioMensual(),PriceCreateParams.Recurring.Interval.MONTH,"mensual",hash);
            Price anual=plan.getPrecioAnual()==null?null:crearPrecio(plan,productoId,plan.getPrecioAnual(),PriceCreateParams.Recurring.Interval.YEAR,"anual",hash);
            plan.setStripeProductoId(productoId); plan.setStripePrecioMensualId(mensual.getId());
            plan.setStripePrecioAnualId(anual==null?null:anual.getId()); plan.setStripeConfigHash(hash); planes.save(plan);
            desactivarPrecioAnterior(precioMensualAnterior,mensual.getId());
            desactivarPrecioAnterior(precioAnualAnterior,anual==null?null:anual.getId());
            auditoria.registrar(usuarioId,"SINCRONIZAR","PLAN_STRIPE",String.valueOf(plan.getId()),
                    Map.of("ambiente",ambiente(),"productoId",productoId));
            return sincronizado(plan,true);
        } catch(StripeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Stripe no pudo sincronizar el plan: "+mensajeStripe(ex),ex);
        }
    }

    @Transactional
    public RedireccionStripeResponse crearCheckout(Long empresaId,Long usuarioId,CrearCheckoutSuscripcionRequest solicitud) {
        validarConfiguracion(false);
        PlanPlataformaEntidad plan=planes.findById(solicitud.planId()).filter(PlanPlataformaEntidad::isActivo)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"El plan no está disponible"));
        String ciclo=solicitud.ciclo()==null?"MENSUAL":solicitud.ciclo();
        String precioId="ANUAL".equals(ciclo)?plan.getStripePrecioAnualId():plan.getStripePrecioMensualId();
        if(!presente(precioId)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Sincroniza este plan con Stripe antes de contratarlo");
        EmpresaEntidad empresa=empresas.findById(empresaId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"El negocio no existe"));
        UsuarioEntidad usuario=usuarios.findByIdAndEmpresaId(usuarioId,empresaId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"El usuario no existe"));
        if(!puedeContratar(empresaId))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"El negocio ya tiene una suscripción; administra el cambio desde el portal");
        try {
            String clienteId=obtenerOCrearCliente(empresa,usuario);
            SuscripcionPlataformaEntidad local=new SuscripcionPlataformaEntidad();
            local.setEmpresaId(empresaId); local.setPlanId(plan.getId()); local.setEstado("PENDIENTE_PAGO"); local.setCiclo(ciclo);
            local.setCorreoFacturacion(usuario.getCorreo());
            local.setPrecioAcordado("ANUAL".equals(ciclo)?plan.getPrecioAnual():plan.getPrecioMensual()); local.setMoneda(plan.getMoneda());
            local.setIniciaEn(LocalDateTime.now(reloj)); local.setStripeClienteId(clienteId); local=suscripciones.save(local);
            SessionCreateParams.SubscriptionData.Builder datosSuscripcion=SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("fluora_empresa_id",String.valueOf(empresaId))
                    .putMetadata("fluora_suscripcion_id",String.valueOf(local.getId()));
            if(plan.getDiasPrueba()>0&&!suscripciones.existsByEmpresaIdAndPruebaHastaIsNotNull(empresaId)) datosSuscripcion.setTrialPeriodDays((long)plan.getDiasPrueba());
            SessionCreateParams params=SessionCreateParams.builder().setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(clienteId).setClientReferenceId(String.valueOf(empresaId))
                    .setSuccessUrl(agregarSessionId(propiedades.checkoutSuccessUrl()))
                    .setCancelUrl(propiedades.checkoutCancelUrl())
                    .putMetadata("fluora_empresa_id",String.valueOf(empresaId))
                    .putMetadata("fluora_suscripcion_id",String.valueOf(local.getId()))
                    .setSubscriptionData(datosSuscripcion.build())
                    .addLineItem(SessionCreateParams.LineItem.builder().setPrice(precioId).setQuantity(1L).build()).build();
            Session sesion=Session.create(params,opciones("checkout-"+local.getId()));
            local.setStripeCheckoutSesionId(sesion.getId()); suscripciones.save(local);
            return new RedireccionStripeResponse(sesion.getUrl());
        } catch(StripeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Stripe no pudo iniciar el checkout: "+mensajeStripe(ex),ex);
        }
    }

    public RedireccionStripeResponse crearPortal(Long empresaId) {
        validarConfiguracion(false);
        StripeClientePlataformaEntidad cliente=clientes.findById(empresaId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"El negocio aún no tiene cliente de Stripe"));
        try {
            Builder builder=com.stripe.param.billingportal.SessionCreateParams.builder()
                    .setCustomer(cliente.getStripeClienteId()).setReturnUrl(propiedades.portalReturnUrl());
            com.stripe.model.billingportal.Session portal=com.stripe.model.billingportal.Session.create(builder.build(),opciones(null));
            return new RedireccionStripeResponse(portal.getUrl());
        } catch(StripeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Stripe no pudo abrir el portal: "+mensajeStripe(ex),ex);
        }
    }

    @Transactional
    public void recibirWebhook(String firma,String payload) {
        validarConfiguracion(true);
        Event event;
        try { event=Webhook.constructEvent(payload,firma,propiedades.webhookSecret()); }
        catch(SignatureVerificationException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Firma de Stripe inválida"); }
        catch(Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Evento de Stripe inválido"); }
        if(Boolean.TRUE.equals(event.getLivemode())!="PRODUCCION".equals(ambiente()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El evento de Stripe corresponde a otro ambiente");
        if(eventos.existsByStripeEventoId(event.getId())) return;
        StripeEventoPlataformaEntidad registro=new StripeEventoPlataformaEntidad();
        registro.setStripeEventoId(event.getId()); registro.setTipo(event.getType()); registro.setModoProduccion(Boolean.TRUE.equals(event.getLivemode())); registro.setEstado("PROCESANDO");
        try { registro=eventos.save(registro); }
        catch(DataIntegrityViolationException duplicado) { return; }
        try {
            JsonNode objeto=objectMapper.readTree(payload).path("data").path("object");
            procesarEvento(event.getType(),objeto,event.getCreated()==null?0:event.getCreated());
            registro.setEstado("PROCESADO"); registro.setProcesadoEn(LocalDateTime.now(reloj));
        } catch(Exception ex) {
            registro.setEstado("ERROR"); registro.setErrorUltimo(recortar(ex.getMessage(),1000)); eventos.save(registro);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No se pudo procesar el evento de Stripe",ex);
        }
        eventos.save(registro);
    }

    @Transactional
    public void conciliarSuscripcion(Long localId,com.techprotech.agenda.modulos.plataforma.infraestructura.stripe.ConsultaSuscripcionStripe.Resultado resultado) {
        var local=suscripciones.findById(localId).orElseThrow();
        var objeto=resultado.suscripcion();
        if(!java.util.Objects.equals(local.getStripeSuscripcionId(),texto(objeto,"id"))
                ||!java.util.Objects.equals(local.getStripeClienteId(),texto(objeto,"customer"))
                ||objeto.path("livemode").asBoolean()!= "PRODUCCION".equals(ambiente()))
            throw new IllegalArgumentException("La consulta Stripe no corresponde a la suscripción y ambiente locales");
        var factura=resultado.factura();
        if("active".equals(texto(objeto,"status"))) {
            if(factura==null||!"paid".equals(texto(factura,"status"))
                    ||!java.util.Objects.equals(texto(objeto,"latest_invoice"),texto(factura,"id"))
                    ||!java.util.Objects.equals(local.getStripeClienteId(),texto(factura,"customer"))
                    ||factura.path("livemode").asBoolean()!=objeto.path("livemode").asBoolean())return;
        }
        long ahora=Instant.now(reloj).getEpochSecond();
        procesarEvento("canceled".equals(texto(objeto,"status"))?"customer.subscription.deleted":"customer.subscription.updated",objeto,ahora);
        if(factura!=null&&"paid".equals(texto(factura,"status")))procesarEvento("invoice.paid",factura,ahora);
    }

    private void procesarEvento(String tipo,JsonNode objeto,long creado) {
        if("checkout.session.completed".equals(tipo)) {
            String sesion=texto(objeto,"id");
            suscripciones.findByStripeCheckoutSesionId(sesion).ifPresent(local->{
                local.setStripeSuscripcionId(texto(objeto,"subscription")); local.setStripeClienteId(texto(objeto,"customer"));
                local.setStripeEstado("checkout_completed"); local.setReferenciaExterna(sesion); suscripciones.save(local);
            });
            return;
        }
        if(tipo.startsWith("customer.subscription.")) {
            String id=texto(objeto,"id");
            SuscripcionPlataformaEntidad local=suscripciones.findByStripeSuscripcionId(id).orElseGet(()->{
                String localId=objeto.path("metadata").path("fluora_suscripcion_id").asText(null);
                return localId==null?null:suscripciones.findById(Long.valueOf(localId)).orElse(null);
            });
            if(local==null)return;
            if("REEMPLAZADA".equals(local.getEstado()))return;
            if("CANCELADA".equals(local.getEstado())&&!"customer.subscription.deleted".equals(tipo))return;
            if(!aceptarEvento(local,creado))return;
            Long planAnterior=local.getPlanId();String estadoAnterior=local.getEstado();
            boolean cancelacionAnterior=local.isCancelarAlFinalPeriodo();
            if(!"customer.subscription.deleted".equals(tipo))planSuscripcion.actualizar(local,objeto);
            String estado=texto(objeto,"status"); local.setStripeSuscripcionId(id); local.setStripeClienteId(texto(objeto,"customer"));
            local.setStripeEstado(estado); local.setEstado(mapearEstado(estado));
            if("past_due".equals(estado)) iniciarGracia(local);
            else if("active".equals(estado)||"trialing".equals(estado)) {local.setGraciaHasta(null);local.setAvisoVencimientoEn(null);}
            long periodo=epochPeriodoActual(objeto); local.setPeriodoActualHasta(periodo>0?LocalDateTime.ofInstant(Instant.ofEpochSecond(periodo),ZoneOffset.UTC):null);
            long finPrueba=objeto.path("trial_end").asLong(0); local.setPruebaHasta(finPrueba>0?LocalDateTime.ofInstant(Instant.ofEpochSecond(finPrueba),ZoneOffset.UTC):null);
            local.setCancelarAlFinalPeriodo(objeto.path("cancel_at_period_end").asBoolean(false));
            if("customer.subscription.deleted".equals(tipo))local.setCanceladaEn(LocalDateTime.now(reloj));
            suscripciones.save(local);
            if(!java.util.Objects.equals(planAnterior,local.getPlanId()))avisos.programar(local,"PLAN",String.valueOf(creado)+":"+local.getPlanId(),null);
            if("PRUEBA".equals(local.getEstado())&&!"PRUEBA".equals(estadoAnterior))avisos.programar(local,"PRUEBA",String.valueOf(local.getPruebaHasta()),null);
            if(local.isCancelarAlFinalPeriodo()&&!cancelacionAnterior)avisos.programar(local,"CANCELACION_PROGRAMADA",String.valueOf(creado),null);
            if("CANCELADA".equals(local.getEstado())&&!"CANCELADA".equals(estadoAnterior))avisos.programar(local,"CANCELADA",id,null);
            return;
        }
        if("invoice.paid".equals(tipo)||"invoice.payment_failed".equals(tipo)) {
            String suscripcionId=texto(objeto,"subscription");
            if(suscripcionId==null)suscripcionId=objeto.path("parent").path("subscription_details").path("subscription").asText(null);
            if(suscripcionId==null)return;
            String facturaId=texto(objeto,"id");
            resolverSuscripcionFactura(objeto,suscripcionId).ifPresent(local->{
                if(!aceptarEvento(local,creado))return;
                local.setStripeUltimaFacturaId(facturaId);
                String correo=texto(objeto,"customer_email");
                if(local.getCorreoFacturacion()==null&&correo!=null)local.setCorreoFacturacion(correo);
                if("invoice.paid".equals(tipo)&&debeActivarPorFactura(objeto,local.getEstado())){
                    if(!"CANCELADA".equals(local.getEstado())&&!"REEMPLAZADA".equals(local.getEstado())) {
                        local.setEstado("ACTIVA");local.setStripeEstado("active");local.setGraciaHasta(null);local.setAvisoVencimientoEn(null);
                    }
                }
                if("invoice.payment_failed".equals(tipo)&&!"CANCELADA".equals(local.getEstado())
                        &&!"REEMPLAZADA".equals(local.getEstado())&&!"PENDIENTE_PAGO".equals(local.getEstado())){
                    iniciarGracia(local);local.setEstado("GRACIA");local.setStripeEstado("past_due");
                }
                suscripciones.save(local);
                if("invoice.paid".equals(tipo)&&"ACTIVA".equals(local.getEstado())&&objeto.path("amount_paid").asLong(0)>0)
                    avisos.programar(local,"PAGO",facturaId,BigDecimal.valueOf(objeto.path("amount_paid").asLong(0),2));
                if("invoice.payment_failed".equals(tipo)&&!"CANCELADA".equals(local.getEstado())&&!"REEMPLAZADA".equals(local.getEstado()))avisos.programar(local,"FALLO",facturaId,null);
            });
        }
    }

    private java.util.Optional<SuscripcionPlataformaEntidad> resolverSuscripcionFactura(JsonNode factura,String stripeId) {
        var vinculada=suscripciones.findByStripeSuscripcionId(stripeId);
        if(vinculada.isPresent())return vinculada;
        // Stripe puede entregar invoice.paid antes de customer.subscription.created.
        var metadata=factura.path("parent").path("subscription_details").path("metadata");
        if(metadata.isMissingNode())metadata=factura.path("subscription_details").path("metadata");
        String id=metadata.path("fluora_suscripcion_id").asText(null);
        String empresaId=metadata.path("fluora_empresa_id").asText(null);
        String clienteId=texto(factura,"customer");
        if(id==null||empresaId==null||clienteId==null)return java.util.Optional.empty();
        Long localId;
        try {localId=Long.valueOf(id);} catch(NumberFormatException ex){return java.util.Optional.empty();}
        return suscripciones.findById(localId).filter(s->"PENDIENTE_PAGO".equals(s.getEstado())
                &&s.getStripeSuscripcionId()==null&&empresaId.equals(String.valueOf(s.getEmpresaId()))
                &&clienteId.equals(s.getStripeClienteId())).map(s->{s.setStripeSuscripcionId(stripeId);return s;});
    }

    private boolean aceptarEvento(SuscripcionPlataformaEntidad local,long creado) {
        if(creado<=0)return true;
        if(local.getStripeUltimoEventoCreado()!=null&&creado<local.getStripeUltimoEventoCreado())return false;
        local.setStripeUltimoEventoCreado(creado);
        return true;
    }

    private void iniciarGracia(SuscripcionPlataformaEntidad local) {
        if(local.getGraciaHasta()!=null)return;
        PlanPlataformaEntidad plan=planes.findById(local.getPlanId())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"El plan de la suscripción no existe"));
        local.setGraciaHasta(LocalDateTime.now(reloj).plusDays(plan.getDiasGracia()));
    }

    private Price crearPrecio(PlanPlataformaEntidad plan,String productoId,BigDecimal importe,
            PriceCreateParams.Recurring.Interval intervalo,String ciclo,String hash) throws StripeException {
        return Price.create(PriceCreateParams.builder().setProduct(productoId).setCurrency(plan.getMoneda().toLowerCase(Locale.ROOT))
                .setUnitAmount(importe.movePointRight(2).setScale(0,RoundingMode.UNNECESSARY).longValueExact())
                .setRecurring(PriceCreateParams.Recurring.builder().setInterval(intervalo).build())
                .putMetadata("fluora_plan_id",String.valueOf(plan.getId())).putMetadata("fluora_ciclo",ciclo).build(),
                opciones("precio-"+plan.getId()+"-"+ciclo+"-"+hash));
    }
    private void desactivarPrecioAnterior(String anterior,String actual) throws StripeException {
        if(presente(anterior)&&!anterior.equals(actual))
            Price.retrieve(anterior,opciones(null)).update(PriceUpdateParams.builder().setActive(false).build(),opciones("desactivar-"+anterior));
    }
    private String obtenerOCrearCliente(EmpresaEntidad empresa,UsuarioEntidad usuario) throws StripeException {
        StripeClientePlataformaEntidad existente=clientes.findById(empresa.getId()).orElse(null);
        if(existente!=null)return existente.getStripeClienteId();
        Customer customer=Customer.create(CustomerCreateParams.builder().setName(empresa.getNombre()).setEmail(usuario.getCorreo())
                .putMetadata("fluora_empresa_id",String.valueOf(empresa.getId())).putMetadata("fluora_slug",empresa.getSlug()).build(),
                opciones("cliente-empresa-"+empresa.getId()));
        StripeClientePlataformaEntidad nuevo=new StripeClientePlataformaEntidad(); nuevo.setEmpresaId(empresa.getId());nuevo.setStripeClienteId(customer.getId());clientes.save(nuevo);
        return customer.getId();
    }
    private RequestOptions opciones(String idempotencia){RequestOptions.RequestOptionsBuilder b=RequestOptions.builder().setApiKey(propiedades.secretKey());if(presente(idempotencia))b.setIdempotencyKey(idempotencia);return b.build();}
    private void validarConfiguracion(boolean webhook){if(!propiedades.habilitado()||!presente(propiedades.secretKey())||(webhook&&!presente(propiedades.webhookSecret())))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Stripe Billing no está configurado");if("PRODUCCION".equals(ambiente())&&propiedades.secretKey().startsWith("sk_test_"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La clave de prueba no puede usarse en producción");if("PRUEBA".equals(ambiente())&&propiedades.secretKey().startsWith("sk_live_"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La clave productiva no puede usarse en pruebas");}
    private String hashPlan(PlanPlataformaEntidad p){try{String valor=String.join("|",p.getMoneda(),p.getPrecioMensual().toPlainString(),String.valueOf(p.getPrecioAnual()));return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private SincronizarPlanStripeResponse sincronizado(PlanPlataformaEntidad p,boolean actualizado){return new SincronizarPlanStripeResponse(p.getId(),p.getStripeProductoId(),p.getStripePrecioMensualId(),p.getStripePrecioAnualId(),actualizado);}
    private String ambiente(){return presente(propiedades.ambiente())?propiedades.ambiente().trim().toUpperCase(Locale.ROOT):"PRUEBA";}
    private static String mapearEstado(String estado){return switch(estado==null?"":estado){case "active"->"ACTIVA";case "trialing"->"PRUEBA";case "past_due"->"GRACIA";case "canceled","incomplete_expired"->"CANCELADA";case "unpaid","paused"->"SUSPENDIDA";default->"PENDIENTE_PAGO";};}
    static long epochPeriodoActual(JsonNode objeto){
        long periodo=objeto.path("current_period_end").asLong(0);
        if(periodo>0)return periodo;
        return objeto.path("items").path("data").path(0).path("current_period_end").asLong(0);
    }
    static boolean debeActivarPorFactura(JsonNode objeto,String estadoActual){
        return objeto.path("amount_paid").asLong(0)>0;
    }
    private static String texto(JsonNode n,String campo){String v=n.path(campo).asText(null);return presente(v)?v:null;}
    private static String limpiarBase(String v){if(!presente(v))return null;return v.trim().replaceAll("/+$","");}
    private static String agregarSessionId(String url){if(!presente(url))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La URL de retorno de Stripe no está configurada");return url+(url.contains("?")?"&":"?")+"session_id={CHECKOUT_SESSION_ID}";}
    private static boolean presente(String v){return v!=null&&!v.isBlank();}
    private static String recortar(String v,int max){if(v==null)return null;return v.length()<=max?v:v.substring(0,max);}
    private static String mensajeStripe(StripeException ex){return ex.getStripeError()!=null&&presente(ex.getStripeError().getMessage())?ex.getStripeError().getMessage():"respuesta rechazada por Stripe";}
}
