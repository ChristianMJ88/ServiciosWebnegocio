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
        if(requerido==null)return true;
        Authentication autenticacion=SecurityContextHolder.getContext().getAuthentication();
        if(autenticacion==null||!(autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario)||usuario.esPlataforma())return true;
        capacidades.exigirModulo(usuario.empresaId(),requerido.value());
        return true;
    }
}
