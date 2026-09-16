package br.com.escola.config;

import br.com.escola.model.Licenca;
import br.com.escola.service.LicencaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LicencaInterceptor implements HandlerInterceptor {

    @Autowired
    private LicencaService licencaService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {

        String uri = request.getRequestURI();

        // Rotas que NUNCA sao bloqueadas
        if (uri.startsWith("/licenca") ||
            uri.startsWith("/login") ||
            uri.startsWith("/logout") ||
            uri.startsWith("/css") ||
            uri.startsWith("/js") ||
            uri.startsWith("/images") ||
            uri.startsWith("/error") ||
            uri.startsWith("/acesso-negado")) {
            return true;
        }

        try {
            Licenca licenca = licencaService.getLicencaAtual();

            if (licenca.isBloqueada()) {
                response.sendRedirect(request.getContextPath() + "/licenca/ativar?bloqueado");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[LicencaInterceptor] Erro: " + e.getMessage());
        }

        return true;
    }
}