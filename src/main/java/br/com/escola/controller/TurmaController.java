package br.com.escola.controller;

import br.com.escola.model.Turma;
import br.com.escola.service.TurmaService;
import br.com.escola.service.SerieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/turmas")
public class TurmaController {

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private SerieService serieService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("turmas", turmaService.listarTodas());
        return "turmas/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("turma", new Turma());
        model.addAttribute("series", serieService.listarTodas());
        return "turmas/cadastrar";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Turma turma) {
        turmaService.salvar(turma);
        return "redirect:/turmas";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Turma turma = turmaService.buscarPorId(id);
        model.addAttribute("turma", turma);
        model.addAttribute("series", serieService.listarTodas());
        return "turmas/cadastrar";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        turmaService.excluir(id);
        return "redirect:/turmas";
    }

    @GetMapping("/alunos")
    public String listarAlunosPorTurma(Model model) {
        model.addAttribute("turmas", turmaService.listarTodasComAlunos());
        return "turmas/alunos-por-turma";
    }
}