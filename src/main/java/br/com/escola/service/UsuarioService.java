package br.com.escola.service;

import br.com.escola.model.Usuario;
import br.com.escola.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario salvar(Usuario usuario) {
        if (usuario.getId() == null) {
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        } else {
            Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(null);
            if (existente != null) {
                if (usuario.getSenha() == null || usuario.getSenha().isEmpty()) {
                    usuario.setSenha(existente.getSenha());
                } else if (!usuario.getSenha().startsWith("$2a$")) {
                    usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
                }
            }
        }
        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    public void excluir(Long id) {
        usuarioRepository.deleteById(id);
    }
}