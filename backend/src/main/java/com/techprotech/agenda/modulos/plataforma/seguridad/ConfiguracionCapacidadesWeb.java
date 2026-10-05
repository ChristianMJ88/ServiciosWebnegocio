package com.techprotech.agenda.modulos.plataforma.seguridad;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionCapacidadesWeb implements WebMvcConfigurer {
    private final InterceptorModuloContratado interceptor;
    public ConfiguracionCapacidadesWeb(InterceptorModuloContratado interceptor){this.interceptor=interceptor;}
    @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(interceptor);}
}
