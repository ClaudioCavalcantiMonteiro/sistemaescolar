package br.com.escola.controller;

import br.com.escola.service.AlunoService;
import br.com.escola.service.EscolaService;
import br.com.escola.service.MateriaService;
import br.com.escola.service.NotaService;
import br.com.escola.service.SerieService;
import br.com.escola.service.TurmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    private SerieService serieService;

    @Autowired
    private MateriaService materiaService;

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private NotaService notaService;

    @Autowired
    private EscolaService escolaService;   // NOVO

    @GetMapping
    public String dashboard(Model model) {
        // Totais
        model.addAttribute("totalAlunos", alunoService.listarTodos().size());
        model.addAttribute("totalSeries", serieService.listarTodas().size());
        model.addAttribute("totalMaterias", materiaService.listarTodas().size());
        model.addAttribute("totalTurmas", turmaService.listarTodas().size());

        // Media geral
        Double mediaGeral = notaService.calcularMediaGeralEscola();
        model.addAttribute("mediaGeral", mediaGeral != null ? String.format("%.1f", mediaGeral) : "0.0");

        // Aprovados e reprovados
        int[] resultado = notaService.contarAprovadosReprovados();
        model.addAttribute("aprovados", resultado[0]);
        model.addAttribute("reprovados", resultado[1]);

        // Dados da escola (para o cabecalho)
        model.addAttribute("escola", escolaService.buscarEscola());   // NOVO

        return "dashboard";
    }

    // ==================================================================
    // API: Alunos por Serie
    // ==================================================================
    @GetMapping("/api/alunos-por-serie")
    @ResponseBody
    public Map<String, Object> alunosPorSerie() {
        Map<String, Object> resultado = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Integer> valores = new ArrayList<>();

        for (var serie : serieService.listarTodas()) {
            long total = alunoService.listarTodos().stream()
                    .filter(a -> a.getSerie() != null && a.getSerie().getId().equals(serie.getId()))
                    .count();
            if (total > 0) {
                labels.add(serie.getNome());
                valores.add((int) total);
            }
        }

        resultado.put("labels", labels);
        resultado.put("valores", valores);
        return resultado;
    }

    // ==================================================================
    // API: Media por Materia
    // ==================================================================
    @GetMapping("/api/media-por-materia")
    @ResponseBody
    public Map<String, Object> mediaPorMateria() {
        Map<String, Object> resultado = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Double> valores = new ArrayList<>();
        List<String> cores = new ArrayList<>();

        String[] paleta = {"#4a6fa5", "#e74c3c", "#27ae60", "#f39c12",
                           "#9b59b6", "#16a085", "#d35400", "#2c3e50",
                           "#e91e63", "#00bcd4"};

        int i = 0;
        for (var materia : materiaService.listarTodas()) {
            Double media = notaService.calcularMediaPorMateria(materia.getId());
            labels.add(materia.getNome());
            valores.add(media != null ? media : 0.0);
            cores.add(paleta[i % paleta.length]);
            i++;
        }

        resultado.put("labels", labels);
        resultado.put("valores", valores);
        resultado.put("cores", cores);
        return resultado;
    }
}