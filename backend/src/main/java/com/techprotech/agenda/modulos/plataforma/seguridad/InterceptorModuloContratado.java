package com.techprotech.agenda.modulos.plataforma.seguridad;

import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InterceptorModuloContratado implements HandlerInterceptor {
    private final ServicioCapacidadesEmpresa capacidades;
    public InterceptorModuloContratado(ServicioCapacidadesEmpresa capacidades){this.capacidades=capacidades;}

    @Override
    public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
        if(!(handler instanceof HandlerMethod metodo))return true;
        RequiereModulo requerido=AnnotatedElementUtils.findMergedAnnotation(metodo.getMethod(),RequiereModulo.class);
        if(requerido==null)requerido=AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(),RequiereModulo.class);

        Authentication autenticacion=SecurityContextHolder.getContext().getAuthentication();
        if(autenticacion==null||!(autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario)||usuario.esPlataforma())return true;
        String ruta=request.getRequestURI().substring(request.getContextPath().length());
        boolean operacion=java.util.List.of("/api/v1/admin/","/api/v1/caja","/api/v1/recepcion","/api/v1/staff/")
                .stream().anyMatch(prefijo->ruta.equals(prefijo)||ruta.startsWith(prefijo.endsWith("/")?prefijo:prefijo+"/"));
        boolean regularizacion=ruta.equals("/api/v1/admin/suscripcion")||ruta.startsWith("/api/v1/admin/suscripcion/");
        if(requerido!=null)capacidades.exigirModulo(usuario.empresaId(),requerido.value());
        else if(operacion&&!regularizacion)capacidades.exigirSuscripcion(usuario.empresaId());
        return true;
    }
}
