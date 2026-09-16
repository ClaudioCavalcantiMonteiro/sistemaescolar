package br.com.escola.controller;

import br.com.escola.model.Usuario;
import br.com.escola.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        u.setRole("PROFESSOR");
        model.addAttribute("usuario", u);
        return "usuarios/cadastrar";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Usuario usuario, RedirectAttributes attributes) {
        try {
            usuarioService.salvar(usuario);
            attributes.addFlashAttribute("mensagemSucesso", "Usuário salvo com sucesso!");
        } catch (DataIntegrityViolationException e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Já existe um usuário cadastrado com este e-mail!");
            if (usuario.getId() != null) {
                return "redirect:/usuarios/editar/" + usuario.getId();
            }
            return "redirect:/usuarios/novo";
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao salvar o usuário: " + e.getMessage());
            return "redirect:/usuarios";
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id);
        model.addAttribute("usuario", usuario);
        return "usuarios/cadastrar";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes attributes) {
        Usuario usuario = usuarioService.buscarPorId(id);

        if (usuario != null && "ADMIN".equals(usuario.getRole())) {
            long admins = usuarioService.listarTodos().stream()
                    .filter(u -> "ADMIN".equals(u.getRole()))
                    .count();
            if (admins <= 1) {
                attributes.addFlashAttribute("mensagemErro",
                        "Não é possível excluir o último administrador do sistema.");
                return "redirect:/usuarios";
            }
        }

        try {
            usuarioService.excluir(id);
            attributes.addFlashAttribute("mensagemSucesso", "Usuário excluído com sucesso!");
        } catch (DataIntegrityViolationException e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Não é possível excluir este usuário, pois ele está vinculado a registros do sistema.");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao excluir: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }
}