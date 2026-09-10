package br.com.escola.config;

import br.com.escola.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Públicas
                .requestMatchers("/login", "/css/**", "/js/**",
                                 "/images/**", "/h2-console/**", "/error").permitAll()

                // Apenas ADMIN
                .requestMatchers("/usuarios/**").hasRole("ADMIN")

                // Cadastros básicos (ADMIN + SECRETARIA)
                .requestMatchers("/series/**").hasAnyRole("ADMIN", "SECRETARIA")
                .requestMatchers("/materias/**").hasAnyRole("ADMIN", "SECRETARIA")
                .requestMatchers("/turmas/**").hasAnyRole("ADMIN", "SECRETARIA")
                .requestMatchers("/alunos/**").hasAnyRole("ADMIN", "SECRETARIA")

                // Notas (ADMIN + PROFESSOR)
                .requestMatchers("/notas/**").hasAnyRole("ADMIN", "PROFESSOR")

                // Frequência (ADMIN + PROFESSOR + SECRETARIA)
                .requestMatchers("/frequencia/**").hasAnyRole("ADMIN", "PROFESSOR", "SECRETARIA")

                // Financeiro (ADMIN + SECRETARIA)
                .requestMatchers("/financeiro/**").hasAnyRole("ADMIN", "SECRETARIA")

                // Relatórios e Dashboard
                .requestMatchers("/relatorios/**", "/dashboard/**")
                    .hasAnyRole("ADMIN", "PROFESSOR", "SECRETARIA")

                // Qualquer outra
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/acesso-negado"))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}