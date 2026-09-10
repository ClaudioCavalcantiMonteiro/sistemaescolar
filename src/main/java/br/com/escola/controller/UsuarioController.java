package br.com.escola.controller;

import br.com.escola.model.Usuario;
import br.com.escola.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String listar(@RequestParam(required = false) String nome,
                         @RequestParam(required = false) String role,
                         Model model) {

        List<Usuario> usuarios = usuarioService.listarTodos();

        if (nome != null && !nome.trim().isEmpty()) {
            String termo = nome.trim().toLowerCase();
            usuarios = usuarios.stream()
                    .filter(u -> u.getNome().toLowerCase().contains(termo) ||
                                 u.getEmail().toLowerCase().contains(termo))
                    .collect(Collectors.toList());
        }

        if (role != null && !role.trim().isEmpty()) {
            usuarios = usuarios.stream()
                    .filter(u -> u.getRole() != null && u.getRole().equals(role))
                    .collect(Collectors.toList());
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("filtroNome", nome);
        model.addAttribute("filtroRole", role);
        model.addAttribute("totalUsuarios", usuarioService.listarTodos().size());
        model.addAttribute("totalFiltrado", usuarios.size());
        return "usuarios/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Usuario u = new Usuario();
        u.setRole("PROFESSOR"); // Valor padrão sugerido
        model.addAttribute("usuario", u);
        return "usuarios/cadastrar";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Usuario usuario) {
        System.out.println(">>> Salvando usuário: " + usuario.getEmail() +
                          " | role recebida: " + usuario.getRole() +
                          " | id: " + usuario.getId());
        usuarioService.salvar(usuario);
        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id);
        model.addAttribute("usuario", usuario);
        return "usuarios/cadastrar";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        Usuario usuario = usuarioService.buscarPorId(id);
        // Impede exclusão do último admin
        if (usuario != null && "ADMIN".equals(usuario.getRole())) {
            long admins = usuarioService.listarTodos().stream()
                    .filter(u -> "ADMIN".equals(u.getRole()))
                    .count();
            if (admins <= 1) {
                return "redirect:/usuarios?erroUltimoAdmin";
            }
        }
        usuarioService.excluir(id);
        return "redirect:/usuarios";
    }
}