package br.com.escola;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.escola.model.Usuario;
import br.com.escola.repository.UsuarioRepository;


@SuppressWarnings("unused")
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Verifica se já existe um usuário com este email
        if (usuarioRepository.findByEmail("admin@escola.com").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setEmail("admin@escola.com");
            admin.setSenha(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            usuarioRepository.save(admin);
            System.out.println("✅ Usuário admin criado: admin@escola.com / senha: admin123");
        } else {
            System.out.println("ℹ️ Usuário admin já existe.");
        }
    }
}