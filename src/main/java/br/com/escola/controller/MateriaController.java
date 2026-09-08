package br.com.escola.controller;

import br.com.escola.model.Materia;
import br.com.escola.service.MateriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/materias")
public class MateriaController {

    @Autowired
    private MateriaService materiaService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("materias", materiaService.listarTodas());
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
        Materia materia = materiaService.buscarPorId(id);
        model.addAttribute("materia", materia);
        return "materias/cadastrar";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        materiaService.excluir(id);
        return "redirect:/materias";
    }
}