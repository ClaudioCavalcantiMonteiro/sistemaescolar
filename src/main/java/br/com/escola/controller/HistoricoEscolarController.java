package br.com.escola.controller;

import br.com.escola.model.Aluno;
import br.com.escola.model.HistoricoEscolar;
import br.com.escola.model.ItemHistorico;
import br.com.escola.service.AlunoService;
import br.com.escola.service.EscolaService;
import br.com.escola.service.HistoricoEscolarService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/historico")
public class HistoricoEscolarController {

    @Autowired
    private HistoricoEscolarService historicoService;

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private EscolaService escolaService;

    @Autowired
    private PdfService pdfService;

    // ==================================================================
    // TELA INICIAL
    // ==================================================================
    @GetMapping
    public String index(@RequestParam(required = false) Long turmaId,
                        @RequestParam(required = false) String nome,
                        Model model) {

        List<Aluno> alunos = alunoService.listarTodos();

        if (turmaId != null && turmaId > 0) {
            alunos = alunos.stream()
                    .filter(a -> a.getTurma() != null && a.getTurma().getId().equals(turmaId))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (nome != null && !nome.trim().isEmpty()) {
            String termo = nome.trim().toLowerCase();
            alunos = alunos.stream()
                    .filter(a -> a.getNome() != null && a.getNome().toLowerCase().contains(termo))
                    .collect(java.util.stream.Collectors.toList());
        }

        model.addAttribute("alunos", alunos);
        model.addAttribute("turmas", turmaService.listarTodas());
        model.addAttribute("turmaId", turmaId);
        model.addAttribute("nome", nome);
        model.addAttribute("anoAtual", LocalDate.now().getYear());
        model.addAttribute("escola", escolaService.buscarEscola());

        return "historico/index";
    }

    // ==================================================================
    // HISTÓRICOS DE UM ALUNO
    // ==================================================================
    @GetMapping("/aluno/{alunoId}")
    public String listarPorAluno(@PathVariable Long alunoId, Model model) {
        Aluno aluno = alunoService.buscarPorId(alunoId);
        if (aluno == null) {
            return "redirect:/historico";
        }

        List<HistoricoEscolar> historicos = historicoService.listarPorAluno(alunoId);

        model.addAttribute("aluno", aluno);
        model.addAttribute("historicos", historicos);
        model.addAttribute("anoAtual", LocalDate.now().getYear());
        model.addAttribute("escola", escolaService.buscarEscola());

        return "historico/aluno";
    }

    // ==================================================================
    // FORM DE GERAÇÃO
    // ==================================================================
    @GetMapping("/gerar/{alunoId}")
    public String formGerar(@PathVariable Long alunoId, Model model) {
        Aluno aluno = alunoService.buscarPorId(alunoId);
        if (aluno == null) {
            return "redirect:/historico";
        }

        model.addAttribute("aluno", aluno);
        model.addAttribute("anoAtual", LocalDate.now().getYear());
        model.addAttribute("escola", escolaService.buscarEscola());

        return "historico/gerar";
    }

    // ==================================================================
    // EXECUTA A GERAÇÃO
    // ==================================================================
    @PostMapping("/gerar")
    public String gerar(@RequestParam Long alunoId,
                        @RequestParam Integer anoLetivo,
                        @RequestParam(required = false) String escolaOrigem,
                        @RequestParam(required = false) String cidadeOrigem,
                        RedirectAttributes attributes) {
        try {
            HistoricoEscolar h;

            boolean isExterno = escolaOrigem != null && !escolaOrigem.trim().isEmpty();

            if (isExterno) {
                h = historicoService.gerarHistoricoExterno(alunoId, anoLetivo,
                        escolaOrigem.trim(),
                        cidadeOrigem != null ? cidadeOrigem.trim() : "");
                attributes.addFlashAttribute("mensagemSucesso",
                        "Histórico de transferência (" + anoLetivo + ") criado! Adicione as matérias abaixo.");
            } else {
                h = historicoService.gerarHistorico(alunoId, anoLetivo);
                attributes.addFlashAttribute("mensagemSucesso",
                        "Histórico de " + anoLetivo + " gerado com sucesso!");
            }

            return "redirect:/historico/" + h.getId() + "#tabela-materias";

        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Erro ao gerar histórico: " + e.getMessage());
            return "redirect:/historico/aluno/" + alunoId;
        }
    }

    // ==================================================================
    // VISUALIZAR / EDITAR
    // ==================================================================
    @GetMapping("/{id}")
    public String editar(@PathVariable Long id, Model model) {
        HistoricoEscolar historico = historicoService.buscarPorId(id);
        if (historico == null) {
            return "redirect:/historico";
        }

        model.addAttribute("historico", historico);
        model.addAttribute("escola", escolaService.buscarEscola());

        return "historico/editar";
    }

    // ==================================================================
    // SALVAR EDIÇÕES
    // ==================================================================
    @PostMapping("/salvar")
    public String salvar(@RequestParam Long id,
                         @RequestParam Map<String, String> params,
                         RedirectAttributes attributes) {
        try {
            HistoricoEscolar historico = historicoService.buscarPorId(id);
            if (historico == null) {
                throw new RuntimeException("Histórico não encontrado: " + id);
            }

            historico.setSerie(params.getOrDefault("serie", historico.getSerie()));
            historico.setTurma(params.getOrDefault("turma", historico.getTurma()));
            historico.setTurno(params.getOrDefault("turno", historico.getTurno()));

            String escolaOrigem = params.get("escolaOrigem");
            historico.setEscolaOrigem(escolaOrigem != null && !escolaOrigem.trim().isEmpty()
                    ? escolaOrigem.trim() : null);

            String cidadeOrigem = params.get("cidadeOrigem");
            historico.setCidadeOrigem(cidadeOrigem != null && !cidadeOrigem.trim().isEmpty()
                    ? cidadeOrigem.trim() : null);

            String situacao = params.get("situacaoFinal");
            if (situacao != null && !situacao.isEmpty()) {
                historico.setSituacaoFinal(situacao);
            }

            historico.setObservacoes(params.get("observacoes"));

            String diasStr = params.get("diasLetivos");
            if (diasStr != null && !diasStr.isEmpty()) {
                try { historico.setDiasLetivos(Integer.parseInt(diasStr)); } catch (Exception ignored) {}
            }

            String mediaStr = params.get("mediaAprovacao");
            if (mediaStr != null && !mediaStr.isEmpty()) {
                try { historico.setMediaAprovacao(Double.parseDouble(mediaStr.replace(",", "."))); } catch (Exception ignored) {}
            }

            if (historico.getItems() != null) {
                for (ItemHistorico item : historico.getItems()) {
                    String prefix = "item_" + item.getId() + "_";

                    String nome = params.get(prefix + "nomeMateria");
                    if (nome != null && !nome.trim().isEmpty()) {
                        item.setNomeMateria(nome.trim());
                    }

                    String nota = params.get(prefix + "notaFinal");
                    if (nota != null && !nota.isEmpty()) {
                        try { item.setNotaFinal(Double.parseDouble(nota.replace(",", "."))); } catch (Exception ignored) {}
                    } else {
                        item.setNotaFinal(null);
                    }

                    String faltas = params.get(prefix + "faltas");
                    if (faltas != null && !faltas.isEmpty()) {
                        try { item.setFaltas(Integer.parseInt(faltas)); } catch (Exception ignored) {}
                    } else {
                        item.setFaltas(0);
                    }

                    String ch = params.get(prefix + "cargaHoraria");
                    if (ch != null && !ch.isEmpty()) {
                        try { item.setCargaHoraria(Integer.parseInt(ch)); } catch (Exception ignored) {}
                    } else {
                        item.setCargaHoraria(80);
                    }

                    String resultado = params.get(prefix + "resultado");
                    if (resultado != null && !resultado.isEmpty()) {
                        item.setResultado(resultado);
                    }
                }
            }

            historicoService.salvar(historico);
            attributes.addFlashAttribute("mensagemSucesso", "Histórico salvo com sucesso!");

        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/historico/" + id + "#tabela-materias";
    }

    // ==================================================================
    // ADICIONAR ITEM
    // ==================================================================
    @GetMapping("/{id}/adicionar-item")
    public String adicionarItem(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            historicoService.adicionarItem(id);
            attributes.addFlashAttribute("mensagemSucesso", "Matéria adicionada! Preencha os dados e salve.");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Erro ao adicionar matéria: " + e.getMessage());
        }
        return "redirect:/historico/" + id + "#tabela-materias";
    }

    // ==================================================================
    // REMOVER ITEM
    // ==================================================================
    @GetMapping("/{historicoId}/remover-item/{itemId}")
    public String removerItem(@PathVariable Long historicoId,
                              @PathVariable Long itemId,
                              RedirectAttributes attributes) {
        try {
            historicoService.removerItem(itemId);
            attributes.addFlashAttribute("mensagemSucesso", "Matéria removida!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Erro ao remover matéria: " + e.getMessage());
        }
        return "redirect:/historico/" + historicoId + "#tabela-materias";
    }

    // ==================================================================
    // RECALCULAR
    // ==================================================================
    @GetMapping("/recalcular/{id}")
    public String recalcular(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            historicoService.recalcular(id);
            attributes.addFlashAttribute("mensagemSucesso",
                    "Histórico recalculado com os dados atuais!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Erro ao recalcular: " + e.getMessage());
        }
        return "redirect:/historico/" + id;
    }

    // ==================================================================
    // EXCLUIR
    // ==================================================================
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            HistoricoEscolar h = historicoService.buscarPorId(id);
            Long alunoId = (h != null && h.getAluno() != null) ? h.getAluno().getId() : null;

            historicoService.excluir(id);
            attributes.addFlashAttribute("mensagemSucesso", "Histórico excluído com sucesso!");

            if (alunoId != null) {
                return "redirect:/historico/aluno/" + alunoId;
            }
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Erro ao excluir: " + e.getMessage());
        }
        return "redirect:/historico";
    }

    // ==================================================================
    // PDF DO HISTÓRICO (1 ANO) — 2 páginas
    // ==================================================================
    @GetMapping("/{id}/pdf")
    public ResponseEntity<InputStreamResource> baixarHistoricoPdf(@PathVariable Long id) {
        HistoricoEscolar h = historicoService.buscarPorId(id);

        String nomeAluno = (h != null && h.getAluno() != null)
                ? h.getAluno().getNome().replaceAll("\\s+", "_")
                : "aluno";
        String ano = (h != null && h.getAnoLetivo() != null) ? String.valueOf(h.getAnoLetivo()) : "xxxx";

        String nomeArquivo = "historico_" + nomeAluno + "_" + ano + ".pdf";

        ByteArrayInputStream pdf = pdfService.gerarHistoricoEscolarPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + nomeArquivo);

        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    // ==================================================================
    // PDF DO HISTÓRICO COMPLETO (TODOS OS ANOS - DETALHADO)
    // ==================================================================
    @GetMapping("/aluno/{alunoId}/pdf-completo")
    public ResponseEntity<InputStreamResource> baixarHistoricoCompletoPdf(@PathVariable Long alunoId) {
        Aluno aluno = alunoService.buscarPorId(alunoId);

        String nomeAluno = (aluno != null)
                ? aluno.getNome().replaceAll("\\s+", "_")
                : "aluno_" + alunoId;

        String nomeArquivo = "historico_completo_" + nomeAluno + ".pdf";

        ByteArrayInputStream pdf = pdfService.gerarHistoricoCompletoPdf(alunoId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + nomeArquivo);

        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    // ==================================================================
    // PDF DO HISTÓRICO OFICIAL COMPACTO (1 FOLHA)
    // ==================================================================
    @GetMapping("/aluno/{alunoId}/pdf-oficial")
    public ResponseEntity<InputStreamResource> baixarHistoricoOficialPdf(@PathVariable Long alunoId) {
        Aluno aluno = alunoService.buscarPorId(alunoId);

        String nomeAluno = (aluno != null)
                ? aluno.getNome().replaceAll("\\s+", "_")
                : "aluno_" + alunoId;

        String nomeArquivo = "historico_oficial_" + nomeAluno + ".pdf";

        ByteArrayInputStream pdf = pdfService.gerarHistoricoOficialPdf(alunoId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + nomeArquivo);

        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    // ==================================================================
    // PDF DO HISTÓRICO OFICIAL FRENTE/VERSO (2 PÁGINAS)
    // ==================================================================
    @GetMapping("/aluno/{alunoId}/pdf-oficial-fv")
    public ResponseEntity<InputStreamResource> baixarHistoricoOficialFrenteVersoPdf(@PathVariable Long alunoId) {
        Aluno aluno = alunoService.buscarPorId(alunoId);

        String nomeAluno = (aluno != null)
                ? aluno.getNome().replaceAll("\\s+", "_")
                : "aluno_" + alunoId;

        String nomeArquivo = "historico_oficial_fv_" + nomeAluno + ".pdf";

        ByteArrayInputStream pdf = pdfService.gerarHistoricoOficialFrenteVersoPdf(alunoId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + nomeArquivo);

        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }
}