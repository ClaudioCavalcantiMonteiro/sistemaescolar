package br.com.escola.controller;

import br.com.escola.model.Aluno;
import br.com.escola.model.Frequencia;
import br.com.escola.model.Materia;
import br.com.escola.model.Turma;
import br.com.escola.service.AlunoService;
import br.com.escola.service.EscolaService;
import br.com.escola.service.FrequenciaService;
import br.com.escola.service.MateriaService;
import br.com.escola.service.PdfService;
import br.com.escola.service.TurmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Controller
@RequestMapping("/frequencia")
public class FrequenciaController {

    @Autowired
    private FrequenciaService frequenciaService;

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private MateriaService materiaService;

    @Autowired
    private EscolaService escolaService;

    @Autowired
    private PdfService pdfService;

    // ========== PAGINA INICIAL ==========
    @GetMapping
    public String index(Model model) {
        model.addAttribute("turmas", turmaService.listarTodas());
        return "frequencia/index";
    }

    // ========== REGISTRAR FREQUENCIA ==========
    @GetMapping("/registrar")
    public String registrar(@RequestParam(required = false) Long turmaId,
                            @RequestParam(required = false) Long materiaId,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                            Model model) {

        model.addAttribute("turmas", turmaService.listarTodas());
        model.addAttribute("materias", materiaService.listarTodas());

        // Precisa de turma E materia para carregar a lista de alunos
        if (turmaId == null || turmaId <= 0 || materiaId == null || materiaId <= 0) {
            model.addAttribute("turmaId", turmaId);
            model.addAttribute("materiaId", materiaId);
            if (data != null) model.addAttribute("data", data);
            return "frequencia/registrar";
        }

        Turma turma = turmaService.buscarPorId(turmaId);
        if (turma == null) {
            return "redirect:/frequencia";
        }

        Materia materia = materiaService.buscarPorId(materiaId);
        if (materia == null) {
            return "redirect:/frequencia";
        }

        if (data == null) {
            data = LocalDate.now();
        }

        List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);
        List<Frequencia> existentes = frequenciaService.listarPorTurmaEData(turmaId, data);

        Map<Long, Frequencia> mapaExistentes = new HashMap<>();
        for (Frequencia f : existentes) {
            boolean mesmaMateria = f.getMateria() != null
                    && f.getMateria().getId().equals(materiaId);
            if (mesmaMateria) {
                mapaExistentes.put(f.getAluno().getId(), f);
            }
        }

        model.addAttribute("turma", turma);
        model.addAttribute("turmaId", turmaId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("materia", materia);
        model.addAttribute("data", data);
        model.addAttribute("alunos", alunos);
        model.addAttribute("mapaExistentes", mapaExistentes);

        return "frequencia/registrar";
    }

    // ========== SALVAR FREQUENCIA ==========
    @PostMapping("/salvar")
    public String salvar(@RequestParam Long turmaId,
                         @RequestParam Long materiaId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         @RequestParam Map<String, String> params) {

        Turma turma = turmaService.buscarPorId(turmaId);
        Materia materia = materiaService.buscarPorId(materiaId);
        List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);

        for (Aluno aluno : alunos) {
            String prefix = "aluno_" + aluno.getId() + "_";
            String presenteStr = params.get(prefix + "presente");
            String justificativa = params.get(prefix + "justificativa");
            String observacao = params.get(prefix + "observacao");

            boolean presente = "true".equals(presenteStr);

            Frequencia frequencia = frequenciaService.buscarPorAlunoTurmaMateriaData(
                    aluno.getId(), turmaId, materiaId, data);
            if (frequencia == null) {
                frequencia = new Frequencia();
                frequencia.setAluno(aluno);
                frequencia.setTurma(turma);
                frequencia.setMateria(materia);
                frequencia.setData(data);
            }

            frequencia.setPresente(presente);
            frequencia.setJustificativa(justificativa);
            frequencia.setObservacao(observacao);

            frequenciaService.salvar(frequencia);
        }

        return "redirect:/frequencia/registrar?turmaId=" + turmaId
                + "&materiaId=" + materiaId
                + "&data=" + data
                + "&sucesso";
    }

    // ========== TELA DE SELECAO ==========
    @GetMapping("/aluno")
    public String selecionarAluno(Model model) {
        model.addAttribute("turmas", turmaService.listarTodas());
        model.addAttribute("alunos", alunoService.listarTodos());
        return "frequencia/aluno-selecionar";
    }

    // ========== FREQUENCIA DE UM ALUNO ==========
    @GetMapping("/aluno/{alunoId}")
    public String frequenciaAluno(@PathVariable Long alunoId, Model model) {
        Aluno aluno = alunoService.buscarPorId(alunoId);
        if (aluno == null) {
            return "redirect:/alunos";
        }

        List<Frequencia> registros = frequenciaService.listarPorAluno(alunoId);
        FrequenciaService.EstatisticasFrequencia estatisticas =
                frequenciaService.calcularEstatisticas(alunoId);

        model.addAttribute("aluno", aluno);
        model.addAttribute("registros", registros);
        model.addAttribute("estatisticas", estatisticas);
        model.addAttribute("escola", escolaService.buscarEscola());
        return "frequencia/aluno";
    }

    // ========== PDF DA FREQUENCIA INDIVIDUAL DO ALUNO ==========
    @GetMapping("/aluno/{alunoId}/pdf")
    public ResponseEntity<InputStreamResource> baixarFrequenciaAlunoPdf(@PathVariable Long alunoId) {
        Aluno aluno = alunoService.buscarPorId(alunoId);
        String nomeArquivo = "frequencia_" +
                (aluno != null ? aluno.getNome().replaceAll("\\s+", "_") : alunoId) + ".pdf";

        ByteArrayInputStream pdf = pdfService.gerarPdfFrequenciaAluno(alunoId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + nomeArquivo);

        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    // ========== RELATORIO MENSAL POR TURMA ==========
    @GetMapping("/relatorio")
    public String relatorio(@RequestParam(required = false) Long turmaId,
                            @RequestParam(required = false) Integer mes,
                            @RequestParam(required = false) Integer ano,
                            Model model) {

        model.addAttribute("turmas", turmaService.listarTodas());
        model.addAttribute("escola", escolaService.buscarEscola());

        if (turmaId == null || turmaId <= 0) {
            return "frequencia/relatorio";
        }

        Turma turma = turmaService.buscarPorId(turmaId);
        if (turma == null) {
            return "redirect:/frequencia";
        }

        if (mes == null || mes < 1 || mes > 12) mes = LocalDate.now().getMonthValue();
        if (ano == null || ano < 2000) ano = LocalDate.now().getYear();

        YearMonth yearMonth = YearMonth.of(ano, mes);
        LocalDate inicio = yearMonth.atDay(1);
        LocalDate fim = yearMonth.atEndOfMonth();

        List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);
        List<Frequencia> registros = frequenciaService.listarPorTurmaEPeriodo(turmaId, inicio, fim);

        Map<Long, FrequenciaService.EstatisticasFrequencia> estatisticasPorAluno = new LinkedHashMap<>();
        for (Aluno aluno : alunos) {
            int total = 0, presencas = 0;
            for (Frequencia f : registros) {
                if (f.getAluno() != null && f.getAluno().getId().equals(aluno.getId())) {
                    total++;
                    if (Boolean.TRUE.equals(f.getPresente())) presencas++;
                }
            }
            double perc = total > 0 ? (presencas * 100.0 / total) : 0;
            perc = Math.round(perc * 10.0) / 10.0;
            estatisticasPorAluno.put(aluno.getId(),
                    new FrequenciaService.EstatisticasFrequencia(total, presencas, total - presencas, perc));
        }

        int totalRegistros = registros.size();
        int totalPresencas = 0;
        for (Frequencia f : registros) {
            if (Boolean.TRUE.equals(f.getPresente())) totalPresencas++;
        }
        double percentualGeral = totalRegistros > 0 ? (totalPresencas * 100.0 / totalRegistros) : 0;
        percentualGeral = Math.round(percentualGeral * 10.0) / 10.0;

        String[] meses = {"", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                          "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};

        model.addAttribute("turma", turma);
        model.addAttribute("alunos", alunos);
        model.addAttribute("mes", mes);
        model.addAttribute("ano", ano);
        model.addAttribute("nomeMes", meses[mes]);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("estatisticasPorAluno", estatisticasPorAluno);
        model.addAttribute("totalRegistros", totalRegistros);
        model.addAttribute("totalPresencas", totalPresencas);
        model.addAttribute("percentualGeral", percentualGeral);

        return "frequencia/relatorio";
    }

    // ========== PDF DO RELATORIO MENSAL ==========
    @GetMapping("/relatorio/pdf")
    public ResponseEntity<InputStreamResource> baixarRelatorioPdf(
            @RequestParam Long turmaId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {

        Turma turma = turmaService.buscarPorId(turmaId);
        String nomeArquivo = "relatorio_frequencia_" +
                (turma != null ? turma.getNome().replaceAll("\\s+", "_") : turmaId) +
                "_" + mes + "_" + ano + ".pdf";

        ByteArrayInputStream pdf = pdfService.gerarRelatorioFrequenciaPdf(turmaId, mes, ano);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + nomeArquivo);

        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }
}