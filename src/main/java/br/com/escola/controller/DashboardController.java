package br.com.escola.controller;

import br.com.escola.model.Aluno;
import br.com.escola.model.Materia;
import br.com.escola.model.Nota;
import br.com.escola.model.Turma;
import br.com.escola.service.AlunoService;
import br.com.escola.service.MateriaService;
import br.com.escola.service.NotaService;
import br.com.escola.service.SerieService;
import br.com.escola.service.TurmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private MateriaService materiaService;

    @Autowired
    private SerieService serieService;

    @Autowired
    private NotaService notaService;

    @GetMapping
    public String dashboard(Model model) {
        // Totais gerais
        int totalAlunos = alunoService.listarTodos().size();
        int totalTurmas = turmaService.listarTodas().size();
        int totalMaterias = materiaService.listarTodas().size();
        int totalSeries = serieService.listarTodas().size();

        model.addAttribute("totalAlunos", totalAlunos);
        model.addAttribute("totalTurmas", totalTurmas);
        model.addAttribute("totalMaterias", totalMaterias);
        model.addAttribute("totalSeries", totalSeries);

        // Média geral da escola
        double mediaGeral = calcularMediaGeral();
        model.addAttribute("mediaGeral", String.format(java.util.Locale.US, "%.1f", mediaGeral));

        // Alunos aprovados/reprovados
        int aprovados = 0;
        int reprovados = 0;
        for (Aluno aluno : alunoService.listarTodos()) {
            List<Nota> notas = notaService.listarPorAluno(aluno.getId());
            if (notas.isEmpty()) continue;

            double soma = 0;
            int count = 0;
            for (Nota n : notas) {
                if (n.getMediaFinal() != null) {
                    soma += n.getMediaFinal();
                    count++;
                }
            }
            if (count > 0) {
                double media = soma / count;
                if (media >= 6) aprovados++;
                else reprovados++;
            }
        }
        model.addAttribute("aprovados", aprovados);
        model.addAttribute("reprovados", reprovados);

        return "dashboard";
    }

    // API: Alunos por série
    @GetMapping("/api/alunos-por-serie")
    @ResponseBody
    public Map<String, Object> alunosPorSerie() {
        Map<String, Object> resultado = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Integer> valores = new ArrayList<>();

        serieService.listarTodas().forEach(serie -> {
            labels.add(serie.getNome());
            valores.add(alunoService.buscarPorSerie(serie.getId()).size());
        });

        resultado.put("labels", labels);
        resultado.put("valores", valores);
        return resultado;
    }

    // API: Média por matéria
    @GetMapping("/api/media-por-materia")
    @ResponseBody
    public Map<String, Object> mediaPorMateria() {
        Map<String, Object> resultado = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Double> valores = new ArrayList<>();

        String[] cores = {"#4a6fa5", "#e74c3c", "#27ae60", "#f39c12", "#9b59b6",
                          "#16a085", "#d35400", "#2c3e50", "#e91e63", "#00bcd4"};
        List<String> coresList = new ArrayList<>();

        for (Materia materia : materiaService.listarTodas()) {
            double soma = 0;
            int count = 0;
            for (Aluno aluno : alunoService.listarTodos()) {
                for (Nota nota : notaService.listarPorAlunoEMateria(aluno.getId(), materia.getId())) {
                    if (nota.getMediaFinal() != null) {
                        soma += nota.getMediaFinal();
                        count++;
                    }
                }
            }
            if (count > 0) {
                labels.add(materia.getNome());
                valores.add(Math.round((soma / count) * 10.0) / 10.0);
                coresList.add(cores[labels.size() % cores.length]);
            }
        }

        resultado.put("labels", labels);
        resultado.put("valores", valores);
        resultado.put("cores", coresList);
        return resultado;
    }

    private double calcularMediaGeral() {
        double soma = 0;
        int count = 0;
        for (Aluno aluno : alunoService.listarTodos()) {
            for (Nota nota : notaService.listarPorAluno(aluno.getId())) {
                if (nota.getMediaFinal() != null) {
                    soma += nota.getMediaFinal();
                    count++;
                }
            }
        }
        return count > 0 ? soma / count : 0;
    }
}