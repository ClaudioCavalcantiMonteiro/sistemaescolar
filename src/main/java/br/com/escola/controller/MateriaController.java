package br.com.escola.controller;

import br.com.escola.model.Materia;
import br.com.escola.service.MateriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/materias")
public class MateriaController {

    @Autowired
    private MateriaService materiaService;

    @GetMapping
    public String listar(@RequestParam(required = false) String nome, Model model) {
        List<Materia> materias = materiaService.listarTodas();

        if (nome != null && !nome.trim().isEmpty()) {
            String termo = nome.trim().toLowerCase();
            materias = materias.stream()
                    .filter(m -> m.getNome().toLowerCase().contains(termo))
                    .collect(Collectors.toList());
        }

        model.addAttribute("materias", materias);
        model.addAttribute("filtroNome", nome);
        model.addAttribute("totalMaterias", materiaService.listarTodas().size());
        model.addAttribute("totalFiltrado", materias.size());
        return "materias/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("materia", new Materia());
        return "materias/cadastrar";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Materia materia, RedirectAttributes attributes) {

        // Se for NOVO cadastro, verifica se já existe
        if (materia.getId() == null && materiaService.existeNome(materia.getNome())) {
            attributes.addFlashAttribute("mensagemErro", "Já existe uma matéria com este nome!");
            return "redirect:/materias/novo";
        }

        // Se for EDIÇÃO, verifica se o novo nome pertence a outra matéria
        if (materia.getId() != null) {
            Materia existente = materiaService.buscarPorId(materia.getId());
            if (existente != null && !existente.getNome().equalsIgnoreCase(materia.getNome())) {
                if (materiaService.existeNome(materia.getNome())) {
                    attributes.addFlashAttribute("mensagemErro", "Já existe uma matéria com este nome!");
                    return "redirect:/materias/editar/" + materia.getId();
                }
            }
        }

        try {
            materiaService.salvar(materia);
            attributes.addFlashAttribute("mensagemSucesso", "Matéria salva com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao salvar a matéria: " + e.getMessage());
        }
        return "redirect:/materias";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("materia", materiaService.buscarPorId(id));
        return "materias/cadastrar";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            materiaService.excluir(id);
            attributes.addFlashAttribute("mensagemSucesso", "Matéria excluída com sucesso!");
        } catch (DataIntegrityViolationException e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Não é possível excluir esta matéria, pois existem notas ou frequências vinculadas.");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao excluir: " + e.getMessage());
        }
        return "redirect:/materias";
    }
}