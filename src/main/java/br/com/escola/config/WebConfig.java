package br.com.escola.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LicencaInterceptor licencaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licencaInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/login", "/logout",
                    "/css/**", "/js/**", "/images/**",
                    "/error", "/acesso-negado"
                );
    }
}
