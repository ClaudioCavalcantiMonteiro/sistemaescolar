package br.com.escola.controller;

import br.com.escola.model.Materia;
import br.com.escola.model.Nota;
import br.com.escola.service.AlunoService;
import br.com.escola.service.MateriaService;
import br.com.escola.service.NotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/relatorios")
public class GraficoController {

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private MateriaService materiaService;

    @Autowired
    private NotaService notaService;

    // Página de relatórios
    @GetMapping
    public String paginaRelatorios(Model model) {
        model.addAttribute("alunos", alunoService.listarTodos());
        model.addAttribute("materias", materiaService.listarTodas());
        return "relatorios/graficos";
    }

    // ========== API: Evolução individual (uma matéria) ==========
    @GetMapping("/api/evolucao/{alunoId}/{materiaId}")
    @ResponseBody
    public Map<String, Object> dadosEvolucao(@PathVariable Long alunoId,
                                             @PathVariable Long materiaId) {
        List<Nota> notas = notaService.listarPorAlunoEMateria(alunoId, materiaId);

        Map<String, Object> resultado = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Double> medias = new ArrayList<>();

        for (int unidade = 1; unidade <= 4; unidade++) {
            labels.add(unidade + "ª Unidade");
            Double media = null;
            for (Nota n : notas) {
                if (n.getUnidade().equals(unidade)) {
                    media = n.getMediaFinal();
                    break;
                }
            }
            medias.add(media != null ? media : 0.0);
        }

        resultado.put("labels", labels);
        resultado.put("medias", medias);
        return resultado;
    }

    // ========== API: Evolução geral (todas as matérias) ==========
    @GetMapping("/api/evolucao-geral/{alunoId}")
    @ResponseBody
    public Map<String, Object> dadosEvolucaoGeral(@PathVariable Long alunoId) {
        Map<String, Object> resultado = new HashMap<>();

        // Labels fixos
        List<String> labels = new ArrayList<>();
        labels.add("1ª Unidade");
        labels.add("2ª Unidade");
        labels.add("3ª Unidade");
        labels.add("4ª Unidade");
        resultado.put("labels", labels);

        // Cores para as linhas
        String[] cores = {"#4a6fa5", "#e74c3c", "#27ae60", "#f39c12", "#9b59b6",
                          "#16a085", "#d35400", "#2c3e50", "#e91e63", "#00bcd4"};

        List<Materia> materias = materiaService.listarTodas();
        List<Map<String, Object>> datasets = new ArrayList<>();

        int corIndex = 0;
        for (Materia materia : materias) {
            List<Nota> notas = notaService.listarPorAlunoEMateria(alunoId, materia.getId());
            if (notas.isEmpty()) continue;

            List<Object> medias = new ArrayList<>();
            boolean temAlgumaNota = false;
            for (int u = 1; u <= 4; u++) {
                Double media = null;
                for (Nota n : notas) {
                    if (n.getUnidade().equals(u)) {
                        media = n.getMediaFinal();
                        break;
                    }
                }
                if (media != null) temAlgumaNota = true;
                medias.add(media); // pode ser null
            }

            if (!temAlgumaNota) continue;

            String cor = cores[corIndex % cores.length];
            Map<String, Object> ds = new HashMap<>();
            ds.put("label", materia.getNome());
            ds.put("data", medias);
            ds.put("borderColor", cor);
            ds.put("backgroundColor", cor + "20");
            ds.put("borderWidth", 2);
            ds.put("tension", 0.3);
            ds.put("pointRadius", 5);
            ds.put("pointHoverRadius", 7);
            ds.put("spanGaps", true);
            datasets.add(ds);
            corIndex++;
        }

        resultado.put("datasets", datasets);
        return resultado;
    }
}