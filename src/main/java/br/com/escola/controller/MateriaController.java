package br.com.escola.controller;

import br.com.escola.model.Materia;
import br.com.escola.service.MateriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String salvar(@ModelAttribute Materia materia) {
        materiaService.salvar(materia);
        return "redirect:/materias";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("materia", materiaService.buscarPorId(id));
        return "materias/cadastrar";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        materiaService.excluir(id);
        return "redirect:/materias";
    }
}