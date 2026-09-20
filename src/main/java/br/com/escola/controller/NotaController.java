package br.com.escola.controller;

import br.com.escola.model.Aluno;
import br.com.escola.model.Materia;
import br.com.escola.model.Nota;
import br.com.escola.service.AlunoService;
import br.com.escola.service.EscolaService;
import br.com.escola.service.MateriaService;
import br.com.escola.service.NotaService;
import br.com.escola.service.PdfService;
import br.com.escola.service.TurmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/notas")
public class NotaController {

    @Autowired
    private NotaService notaService;

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private MateriaService materiaService;

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EscolaService escolaService;

    // ==================== TELA INICIAL DE LANCAMENTO ====================
    @GetMapping("/lancar")
    public String lancar(@RequestParam(required = false) Long turmaId,
                         @RequestParam(required = false) Long materiaId,
                         @RequestParam(required = false) Integer unidade,
                         @RequestParam(required = false) Integer ano,
                         Model model) {

        if (ano == null) ano = LocalDate.now().getYear();

        model.addAttribute("turmas", turmaService.listarTodas());
        model.addAttribute("materias", materiaService.listarTodas());
        model.addAttribute("ano", ano);
        model.addAttribute("anoAtual", LocalDate.now().getYear());

        if (turmaId != null && turmaId > 0 && materiaId != null && materiaId > 0) {
            return carregarAlunosParaLancamento(turmaId, materiaId, unidade, ano, model);
        }

        model.addAttribute("turmaId", turmaId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("unidade", unidade);
        return "notas/lancar";
    }

    private String carregarAlunosParaLancamento(Long turmaId, Long materiaId,
                                                 Integer unidade, Integer ano, Model model) {
        if (unidade == null) unidade = 1;
        if (ano == null) ano = LocalDate.now().getYear();

        List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);

        Map<Long, Nota> notasPorAluno = new HashMap<>();
        for (Aluno aluno : alunos) {
            List<Nota> notas = notaService.buscarPorAlunoMateriaUnidadeEAno(
                    aluno.getId(), materiaId, unidade, ano);
            if (!notas.isEmpty()) {
                notasPorAluno.put(aluno.getId(), notas.get(0));
            }
        }

        model.addAttribute("turmaSelecionada", turmaService.buscarPorId(turmaId));
        model.addAttribute("materiaSelecionada", materiaService.buscarPorId(materiaId));
        model.addAttribute("unidadeSelecionada", unidade);
        model.addAttribute("anoSelecionado", ano);
        model.addAttribute("alunos", alunos);
        model.addAttribute("notasPorAluno", notasPorAluno);
        model.addAttribute("turmaId", turmaId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("unidade", unidade);
        model.addAttribute("ano", ano);
        model.addAttribute("anoAtual", LocalDate.now().getYear());

        return "notas/lancar-turma";
    }

    // ==================== SALVAR EM LOTE ====================
    @PostMapping("/salvar-lote")
    public String salvarLote(@RequestParam Long turmaId,
                              @RequestParam Long materiaId,
                              @RequestParam Integer unidade,
                              @RequestParam(required = false) Integer ano,
                              @RequestParam Map<String, String> params) {

        if (ano == null) ano = LocalDate.now().getYear();

        List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);
        Materia materia = materiaService.buscarPorId(materiaId);

        int salvas = 0;
        for (Aluno aluno : alunos) {
            String prefix = "nota_" + aluno.getId() + "_";
            String n1 = params.get(prefix + "1");
            String n2 = params.get(prefix + "2");
            String n3 = params.get(prefix + "3");
            String n4 = params.get(prefix + "4");
            String rec = params.get(prefix + "rec");

            boolean temValor = (n1 != null && !n1.isEmpty()) ||
                               (n2 != null && !n2.isEmpty()) ||
                               (n3 != null && !n3.isEmpty()) ||
                               (n4 != null && !n4.isEmpty()) ||
                               (rec != null && !rec.isEmpty());

            if (!temValor) continue;

            List<Nota> existentes = notaService.buscarPorAlunoMateriaUnidadeEAno(
                    aluno.getId(), materiaId, unidade, ano);
            Nota nota;
            if (!existentes.isEmpty()) {
                nota = existentes.get(0);
            } else {
                nota = new Nota();
                nota.setAluno(aluno);
                nota.setMateria(materia);
                nota.setUnidade(unidade);
                nota.setAnoLetivo(ano);
            }

            nota.setNota1(parseDouble(n1));
            nota.setNota2(parseDouble(n2));
            nota.setNota3(parseDouble(n3));
            nota.setNota4(parseDouble(n4));
            nota.setRecuperacao(parseDouble(rec));

            notaService.salvar(nota);
            salvas++;
        }

        return "redirect:/notas/lancar?turmaId=" + turmaId +
               "&materiaId=" + materiaId +
               "&unidade=" + unidade +
               "&ano=" + ano +
               "&loteSucesso=" + salvas;
    }

    // ==================== SALVAR UMA NOTA ====================
    @PostMapping("/salvar")
    public String salvar(@RequestParam("aluno.id") Long alunoId,
                         @RequestParam("materia.id") Long materiaId,
                         @RequestParam("unidade") Integer unidade,
                         @RequestParam(value = "ano", required = false) Integer ano,
                         @RequestParam(value = "nota1", required = false) Double nota1,
                         @RequestParam(value = "nota2", required = false) Double nota2,
                         @RequestParam(value = "nota3", required = false) Double nota3,
                         @RequestParam(value = "nota4", required = false) Double nota4,
                         @RequestParam(value = "recuperacao", required = false) Double recuperacao) {

        if (ano == null) ano = LocalDate.now().getYear();

        List<Nota> existentes = notaService.buscarPorAlunoMateriaUnidadeEAno(
                alunoId, materiaId, unidade, ano);
        Nota nota;
        if (!existentes.isEmpty()) {
            nota = existentes.get(0);
        } else {
            nota = new Nota();
            nota.setAluno(alunoService.buscarPorId(alunoId));
            nota.setMateria(materiaService.buscarPorId(materiaId));
            nota.setUnidade(unidade);
            nota.setAnoLetivo(ano);
        }
        nota.setNota1(nota1);
        nota.setNota2(nota2);
        nota.setNota3(nota3);
        nota.setNota4(nota4);
        nota.setRecuperacao(recuperacao);
        notaService.salvar(nota);

        return "redirect:/notas/lancar?sucesso";
    }

    // ==================== TELA DE SELECAO DE TURMA ====================
    @GetMapping("/turma")
    public String selecionarTurma(Model model) {
        model.addAttribute("turmas", turmaService.listarTodasComAlunos());
        return "notas/turma-selecionar";
    }

    // ==================== NOTAS POR TURMA ====================
    @GetMapping("/turma/{turmaId}")
    public String listarNotasPorTurma(@PathVariable Long turmaId,
                                       @RequestParam(required = false) Integer ano,
                                       Model model) {
        if (ano == null) ano = LocalDate.now().getYear();

        model.addAttribute("turma", turmaService.buscarPorId(turmaId));
        List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);
        model.addAttribute("alunos", alunos);
        List<Materia> materias = materiaService.listarTodas();
        model.addAttribute("materias", materias);
        model.addAttribute("ano", ano);
        model.addAttribute("anoAtual", LocalDate.now().getYear());

        Map<Long, Map<Long, Map<Integer, Nota>>> notasPorAlunoMateria = new HashMap<>();
        Map<Long, Double> mediaGeralPorAluno = new HashMap<>();

        for (Aluno aluno : alunos) {
            Map<Long, Map<Integer, Nota>> porMateria = new HashMap<>();
            List<Nota> notasAluno = notaService.listarPorAlunoEAno(aluno.getId(), ano);

            for (Nota n : notasAluno) {
                if (n.getMateria() == null) continue;
                porMateria.computeIfAbsent(n.getMateria().getId(), k -> new HashMap<>())
                        .put(n.getUnidade(), n);
            }
            notasPorAlunoMateria.put(aluno.getId(), porMateria);

            double soma = 0;
            int count = 0;
            for (Map<Integer, Nota> unidadeMap : porMateria.values()) {
                for (Nota n : unidadeMap.values()) {
                    Double media = n.getMediaFinal();
                    if (media != null) {
                        soma += media;
                        count++;
                    }
                }
            }
            mediaGeralPorAluno.put(aluno.getId(), count > 0 ? soma / count : 0.0);
        }

        model.addAttribute("notasPorAlunoMateria", notasPorAlunoMateria);
        model.addAttribute("mediaGeralPorAluno", mediaGeralPorAluno);
        return "notas/turma";
    }

    // ==================== BOLETIM INDIVIDUAL ====================
    @GetMapping("/boletim/{alunoId}")
    public String boletim(@PathVariable Long alunoId,
                          @RequestParam(required = false) Integer ano,
                          Model model) {
        if (ano == null) ano = LocalDate.now().getYear();

        Aluno aluno = alunoService.buscarPorId(alunoId);
        List<Materia> materias = materiaService.listarTodas();
        List<Nota> notas = notaService.listarPorAlunoEAno(alunoId, ano);

        Map<Long, Map<Integer, Nota>> notasPorMateria = new HashMap<>();
        for (Nota n : notas) {
            if (n.getMateria() == null) continue;
            notasPorMateria.computeIfAbsent(n.getMateria().getId(), k -> new HashMap<>())
                    .put(n.getUnidade(), n);
        }

        StringBuilder datasetsJson = new StringBuilder("[");
        String[] cores = {"#4a6fa5", "#e74c3c", "#27ae60", "#f39c12", "#9b59b6",
                          "#16a085", "#d35400", "#2c3e50", "#e91e63", "#00bcd4"};

        int corIndex = 0;
        boolean primeiro = true;

        for (Materia materia : materias) {
            Map<Integer, Nota> mapaUnidades = notasPorMateria.get(materia.getId());
            if (mapaUnidades == null || mapaUnidades.isEmpty()) continue;

            StringBuilder medias = new StringBuilder("[");
            boolean temAlgumaNota = false;
            for (int u = 1; u <= 4; u++) {
                Nota n = mapaUnidades.get(u);
                if (n != null && n.getMediaFinal() != null) {
                    medias.append(String.format(Locale.US, "%.1f", n.getMediaFinal()));
                    temAlgumaNota = true;
                } else {
                    medias.append("null");
                }
                if (u < 4) medias.append(",");
            }
            medias.append("]");

            if (!temAlgumaNota) continue;

            if (!primeiro) datasetsJson.append(",");
            primeiro = false;

            String cor = cores[corIndex % cores.length];
            datasetsJson.append("{")
                .append("\"label\":\"").append(materia.getNome()).append("\",")
                .append("\"data\":").append(medias).append(",")
                .append("\"borderColor\":\"").append(cor).append("\",")
                .append("\"backgroundColor\":\"").append(cor).append("20\",")
                .append("\"borderWidth\":2,")
                .append("\"tension\":0.3,")
                .append("\"pointRadius\":5,")
                .append("\"pointHoverRadius\":7,")
                .append("\"spanGaps\":true")
                .append("}");
            corIndex++;
        }
        datasetsJson.append("]");

        model.addAttribute("aluno", aluno);
        model.addAttribute("materias", materias);
        model.addAttribute("notasPorMateria", notasPorMateria);
        model.addAttribute("ano", ano);
        model.addAttribute("anoAtual", LocalDate.now().getYear());
        model.addAttribute("graficoLabels", "[\"1ª Unidade\",\"2ª Unidade\",\"3ª Unidade\",\"4ª Unidade\"]");
        model.addAttribute("graficoDatasets", datasetsJson.toString());
        model.addAttribute("escola", escolaService.buscarEscola());

        return "notas/boletim";
    }

    // ==================== BAIXAR PDF ====================
    @GetMapping("/boletim/{alunoId}/pdf")
    public ResponseEntity<InputStreamResource> baixarBoletimPdf(@PathVariable Long alunoId) {
        Aluno aluno = alunoService.buscarPorId(alunoId);
        String nomeArquivo = "boletim_" +
                (aluno != null ? aluno.getNome().replaceAll("\\s+", "_") : alunoId) + ".pdf";

        ByteArrayInputStream pdf = pdfService.gerarBoletimPdf(alunoId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + nomeArquivo);

        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    // ==================== EDITAR NOTAS ====================
    @GetMapping("/editar/{alunoId}")
    public String editarNotas(@PathVariable Long alunoId,
                              @RequestParam(required = false) Integer ano,
                              Model model) {
        if (ano == null) ano = LocalDate.now().getYear();

        Aluno aluno = alunoService.buscarPorId(alunoId);
        List<Nota> notas = notaService.listarPorAlunoEAno(alunoId, ano);
        List<Materia> materias = materiaService.listarTodas();

        model.addAttribute("aluno", aluno);
        model.addAttribute("materias", materias);
        model.addAttribute("ano", ano);
        model.addAttribute("anoAtual", LocalDate.now().getYear());

        Map<Long, Map<Integer, Nota>> notasMap = new HashMap<>();
        for (Nota n : notas) {
            if (n.getMateria() == null) continue;
            notasMap.computeIfAbsent(n.getMateria().getId(), k -> new HashMap<>())
                    .put(n.getUnidade(), n);
        }
        model.addAttribute("notasMap", notasMap);
        return "notas/editar";
    }

    // ==================== SALVAR TODAS AS NOTAS ====================
    @PostMapping("/salvar-todas")
    public String salvarTodas(@RequestParam Long alunoId,
                              @RequestParam(required = false) Integer ano,
                              @RequestParam Map<String, String> params) {
        if (ano == null) ano = LocalDate.now().getYear();

        Aluno aluno = alunoService.buscarPorId(alunoId);

        // ⚠️ Exclui APENAS as notas do aluno NO ANO ESPECÍFICO
        notaService.excluirPorAlunoIdEAno(alunoId, ano);

        int notasCriadas = 0;
        for (Materia materia : materiaService.listarTodas()) {
            for (int unidade = 1; unidade <= 4; unidade++) {
                String prefix = "notas[" + materia.getId() + "][" + unidade + "].";
                String nota1Str = params.get(prefix + "nota1");
                String nota2Str = params.get(prefix + "nota2");
                String nota3Str = params.get(prefix + "nota3");
                String nota4Str = params.get(prefix + "nota4");
                String recStr = params.get(prefix + "recuperacao");

                boolean hasValue = (nota1Str != null && !nota1Str.isEmpty()) ||
                        (nota2Str != null && !nota2Str.isEmpty()) ||
                        (nota3Str != null && !nota3Str.isEmpty()) ||
                        (nota4Str != null && !nota4Str.isEmpty()) ||
                        (recStr != null && !recStr.isEmpty());

                if (hasValue) {
                    Nota nota = new Nota();
                    nota.setAluno(aluno);
                    nota.setMateria(materia);
                    nota.setUnidade(unidade);
                    nota.setAnoLetivo(ano);
                    nota.setNota1(parseDouble(nota1Str));
                    nota.setNota2(parseDouble(nota2Str));
                    nota.setNota3(parseDouble(nota3Str));
                    nota.setNota4(parseDouble(nota4Str));
                    nota.setRecuperacao(parseDouble(recStr));

                    if (nota.getNota1() != null || nota.getNota2() != null || nota.getNota3() != null ||
                            nota.getNota4() != null || nota.getRecuperacao() != null) {
                        notaService.salvar(nota);
                        notasCriadas++;
                    }
                }
            }
        }
        return "redirect:/notas/boletim/" + alunoId + "?ano=" + ano + "&sucesso";
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}