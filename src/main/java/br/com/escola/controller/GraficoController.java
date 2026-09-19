package br.com.escola.controller;

import br.com.escola.model.Materia;
import br.com.escola.model.Nota;
import br.com.escola.service.AlunoService;
import br.com.escola.service.EscolaService;
import br.com.escola.service.MateriaService;
import br.com.escola.service.NotaService;
import br.com.escola.service.PdfService;
import br.com.escola.service.SerieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private SerieService serieService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EscolaService escolaService;   // NOVO

    @GetMapping
    public String paginaRelatorios(Model model) {
        model.addAttribute("alunos", alunoService.listarTodos());
        model.addAttribute("materias", materiaService.listarTodas());
        model.addAttribute("series", serieService.listarTodas());
        model.addAttribute("escola", escolaService.buscarEscola());   // NOVO
        return "relatorios/graficos";
    }

    @GetMapping("/api/alunos-por-serie/{serieId}")
    @ResponseBody
    public List<Map<String, Object>> alunosPorSerie(@PathVariable Long serieId) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (var aluno : alunoService.buscarPorSerie(serieId)) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", aluno.getId());
            item.put("nome", aluno.getNome());
            resultado.add(item);
        }
        return resultado;
    }

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

    @GetMapping("/api/evolucao-geral/{alunoId}")
    @ResponseBody
    public Map<String, Object> dadosEvolucaoGeral(@PathVariable Long alunoId) {
        Map<String, Object> resultado = new HashMap<>();

        List<String> labels = new ArrayList<>();
        labels.add("1ª Unidade");
        labels.add("2ª Unidade");
        labels.add("3ª Unidade");
        labels.add("4ª Unidade");
        resultado.put("labels", labels);

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
                medias.add(media);
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

    // ==================================================================
    // DECLARACAO DE MATRICULA (PDF)
    // ==================================================================
    @GetMapping("/declaracao-matricula/{alunoId}")
    public ResponseEntity<byte[]> declaracaoMatricula(@PathVariable Long alunoId) {
        try {
            var aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                System.err.println("[PDF] Aluno nao encontrado: " + alunoId);
                return ResponseEntity.notFound().build();
            }

            String nomeArquivo = "declaracao_matricula_" +
                    aluno.getNome().replaceAll("\\s+", "_") + ".pdf";

            System.out.println("[PDF] Gerando declaracao para: " + aluno.getNome());

            ByteArrayInputStream pdfStream = pdfService.gerarDeclaracaoMatricula(alunoId);
            byte[] bytes = pdfStream.readAllBytes();

            System.out.println("[PDF] Tamanho do PDF: " + bytes.length + " bytes");

            if (bytes.length == 0) {
                System.err.println("[PDF] ERRO: PDF vazio!");
                return ResponseEntity.status(500).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=" + nomeArquivo);
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(bytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);
        } catch (Exception e) {
            System.err.println("[PDF] ERRO: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}