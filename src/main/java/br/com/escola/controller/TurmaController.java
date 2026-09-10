package br.com.escola.controller;

import br.com.escola.model.Turma;
import br.com.escola.service.SerieService;
import br.com.escola.service.TurmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/turmas")
public class TurmaController {

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private SerieService serieService;

    // Lista com filtros
    @GetMapping
    public String listar(@RequestParam(required = false) String nome,
                         @RequestParam(required = false) Long serieId,
                         Model model) {

        List<Turma> turmas = turmaService.listarTodas();

        if (nome != null && !nome.trim().isEmpty()) {
            String termo = nome.trim().toLowerCase();
            turmas = turmas.stream()
                    .filter(t -> t.getNome().toLowerCase().contains(termo))
                    .collect(Collectors.toList());
        }

        if (serieId != null && serieId > 0) {
            turmas = turmas.stream()
                    .filter(t -> t.getSerie() != null && t.getSerie().getId().equals(serieId))
                    .collect(Collectors.toList());
        }

        model.addAttribute("turmas", turmas);
        model.addAttribute("series", serieService.listarTodas());
        model.addAttribute("filtroNome", nome);
        model.addAttribute("filtroSerieId", serieId);
        model.addAttribute("totalTurmas", turmaService.listarTodas().size());
        model.addAttribute("totalFiltrado", turmas.size());

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

    // Página "Alunos por Turma" (cards de todas as turmas)
    @GetMapping("/alunos")
    public String listarAlunosPorTurma(Model model) {
        model.addAttribute("turmas", turmaService.listarTodasComAlunos());
        return "turmas/alunos-por-turma";
    }

    // NOVO: Página com os alunos de uma turma específica
    @GetMapping("/{id}/alunos")
    public String alunosDaTurma(@PathVariable Long id, Model model) {
        Turma turma = turmaService.buscarPorId(id);
        if (turma == null) {
            return "redirect:/turmas/alunos";
        }
        model.addAttribute("turma", turma);
        model.addAttribute("alunos", turmaService.buscarAlunosPorTurma(id));
        return "turmas/alunos-da-turma";
    }
}