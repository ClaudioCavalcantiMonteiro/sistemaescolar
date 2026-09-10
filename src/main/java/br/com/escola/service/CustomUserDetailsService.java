package br.com.escola.service;

import br.com.escola.model.Usuario;
import br.com.escola.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        String role = usuario.getRole();
        if (role == null || role.isEmpty()) {
            role = "USER";
        }
        role = role.replace("ROLE_", "");

        System.out.println("✅ LOGIN: " + usuario.getEmail() + " | Role: ROLE_" + role);

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenha())
                .roles(role)
                .build();
    }
}