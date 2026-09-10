package br.com.escola.controller;

import br.com.escola.model.Aluno;
import br.com.escola.model.Materia;
import br.com.escola.model.Nota;
import br.com.escola.service.AlunoService;
import br.com.escola.service.MateriaService;
import br.com.escola.service.NotaService;
import br.com.escola.service.TurmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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

    // ========== LANÇAR NOTAS ==========
    @GetMapping("/lancar")
    public String lancar(@RequestParam(required = false) Long turmaId, Model model) {
        model.addAttribute("turmas", turmaService.listarTodas());
        List<Aluno> alunos = (turmaId != null && turmaId > 0) ?
                alunoService.buscarPorTurma(turmaId) : alunoService.listarTodos();
        model.addAttribute("alunos", alunos);
        model.addAttribute("materias", materiaService.listarTodas());
        model.addAttribute("turmaId", turmaId);
        return "notas/lancar";
    }

    // ========== SALVAR UMA NOTA (lançamento individual) ==========
    @PostMapping("/salvar")
    public String salvar(@RequestParam("aluno.id") Long alunoId,
                         @RequestParam("materia.id") Long materiaId,
                         @RequestParam("unidade") Integer unidade,
                         @RequestParam(value = "nota1", required = false) Double nota1,
                         @RequestParam(value = "nota2", required = false) Double nota2,
                         @RequestParam(value = "nota3", required = false) Double nota3,
                         @RequestParam(value = "nota4", required = false) Double nota4,
                         @RequestParam(value = "recuperacao", required = false) Double recuperacao) {

        System.out.println(">>> Salvando nota: aluno=" + alunoId + ", materia=" + materiaId + ", unidade=" + unidade);

        List<Nota> existentes = notaService.buscarPorAlunoMateriaEUnidade(alunoId, materiaId, unidade);
        Nota nota;
        if (!existentes.isEmpty()) {
            nota = existentes.get(0);
        } else {
            nota = new Nota();
            nota.setAluno(alunoService.buscarPorId(alunoId));
            nota.setMateria(materiaService.buscarPorId(materiaId));
            nota.setUnidade(unidade);
        }
        nota.setNota1(nota1);
        nota.setNota2(nota2);
        nota.setNota3(nota3);
        nota.setNota4(nota4);
        nota.setRecuperacao(recuperacao);
        notaService.salvar(nota);

        return "redirect:/notas/lancar?sucesso";
    }

    // ========== BOLETIM ==========
    @GetMapping("/boletim/{alunoId}")
    public String boletim(@PathVariable Long alunoId, Model model) {
        Aluno aluno = alunoService.buscarPorId(alunoId);
        List<Materia> materias = materiaService.listarTodas();
        List<Nota> notas = notaService.listarPorAluno(alunoId);

        Map<Long, Map<Integer, Nota>> notasPorMateria = new HashMap<>();
        for (Nota n : notas) {
            notasPorMateria.computeIfAbsent(n.getMateria().getId(), k -> new HashMap<>())
                    .put(n.getUnidade(), n);
        }

        // ===== Gerar JSON com locale US (ponto decimal) =====
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
                    // 👇 CORREÇÃO: força locale US para usar ponto decimal
                    medias.append(String.format(java.util.Locale.US, "%.1f", n.getMediaFinal()));
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
        model.addAttribute("graficoLabels", "[\"1ª Unidade\",\"2ª Unidade\",\"3ª Unidade\",\"4ª Unidade\"]");
        model.addAttribute("graficoDatasets", datasetsJson.toString());

        return "notas/boletim";
    
    }

    // ========== LISTAR NOTAS POR TURMA ==========
    @GetMapping("/turma/{turmaId}")
    public String listarNotasPorTurma(@PathVariable Long turmaId, Model model) {
        model.addAttribute("turma", turmaService.buscarPorId(turmaId));
        List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);
        model.addAttribute("alunos", alunos);
        List<Materia> materias = materiaService.listarTodas();
        model.addAttribute("materias", materias);

        Map<Long, Map<Long, Map<Integer, Nota>>> notasPorAlunoMateria = new HashMap<>();
        Map<Long, Double> mediaGeralPorAluno = new HashMap<>();

        for (Aluno aluno : alunos) {
            Map<Long, Map<Integer, Nota>> porMateria = new HashMap<>();
            List<Nota> notasAluno = notaService.listarPorAluno(aluno.getId());

            for (Nota n : notasAluno) {
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

    // ========== EDITAR NOTAS ==========
    @GetMapping("/editar/{alunoId}")
    public String editarNotas(@PathVariable Long alunoId, Model model) {
        Aluno aluno = alunoService.buscarPorId(alunoId);
        List<Nota> notas = notaService.listarPorAluno(alunoId);
        List<Materia> materias = materiaService.listarTodas();

        model.addAttribute("aluno", aluno);
        model.addAttribute("materias", materias);

        Map<Long, Map<Integer, Nota>> notasMap = new HashMap<>();
        for (Nota n : notas) {
            notasMap.computeIfAbsent(n.getMateria().getId(), k -> new HashMap<>())
                    .put(n.getUnidade(), n);
        }
        model.addAttribute("notasMap", notasMap);
        return "notas/editar";
    }

    // ========== SALVAR TODAS AS NOTAS (RECRIA TUDO) ==========
    @PostMapping("/salvar-todas")
    public String salvarTodas(@RequestParam Long alunoId,
                              @RequestParam Map<String, String> params) {
        System.out.println("========== INICIANDO SALVAR-TODAS ==========");
        System.out.println("Aluno ID recebido: " + alunoId);

        Aluno aluno = alunoService.buscarPorId(alunoId);
        if (aluno == null) {
            System.out.println("ERRO: Aluno não encontrado!");
            return "redirect:/notas/lancar";
        }

        // ===== PASSO 1: Excluir todas as notas existentes do aluno =====
        notaService.excluirPorAlunoId(alunoId);
        System.out.println("Notas antigas excluídas.");

        // ===== PASSO 2: Recriar todas as notas com base no formulário =====
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

        System.out.println("Notas recriadas: " + notasCriadas);
        System.out.println("========== FIM SALVAR-TODAS ==========");

        return "redirect:/notas/boletim/" + alunoId + "?sucesso";
    }

    // ========== AUXILIAR ==========
    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}