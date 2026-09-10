package br.com.escola;

import br.com.escola.model.Usuario;
import br.com.escola.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByEmail("admin@escola.com").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setEmail("admin@escola.com");
            admin.setSenha(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            usuarioRepository.save(admin);

            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║  ✅ USUÁRIO ADMIN CRIADO                         ║");
            System.out.println("║                                                  ║");
            System.out.println("║  Email: admin@escola.com                         ║");
            System.out.println("║  Senha: admin123                                 ║");
            System.out.println("║                                                  ║");
            System.out.println("║  ⚠️  ALTERE A SENHA APÓS O PRIMEIRO LOGIN         ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
        } else {
            System.out.println("ℹ️  Usuário admin já existe.");
        }
    }
}