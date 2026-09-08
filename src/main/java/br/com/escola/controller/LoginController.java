package br.com.escola.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import br.com.escola.model.Usuario;
import br.com.escola.service.UsuarioService;

@SuppressWarnings("unused")
@Controller
public class LoginController {

    private final UsuarioService usuarioService;

    LoginController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }  // Deve ser injetado

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    @PostMapping("/registro")
    public String salvarRegistro(Usuario usuario) {
        usuario.setRole("USER");
        usuarioService.salvar(usuario);  // Essa linha causa o NPE se o service for null
        return "redirect:/login?registroSucesso";
    }
}