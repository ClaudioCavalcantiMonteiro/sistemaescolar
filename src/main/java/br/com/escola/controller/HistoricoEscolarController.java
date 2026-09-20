package br.com.escola.controller;

import br.com.escola.model.Aluno;
import br.com.escola.model.HistoricoEscolar;
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
                        RedirectAttributes attributes) {
        try {
            HistoricoEscolar h = historicoService.gerarHistorico(alunoId, anoLetivo);
            attributes.addFlashAttribute("mensagemSucesso",
                    "Histórico de " + anoLetivo + " gerado com sucesso!");
            return "redirect:/historico/" + h.getId();
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
    public String salvar(@ModelAttribute HistoricoEscolar historico,
                         RedirectAttributes attributes) {
        try {
            HistoricoEscolar existente = historicoService.buscarPorId(historico.getId());
            if (existente != null) {
                historico.setAluno(existente.getAluno());
                historico.setEscola(existente.getEscola());
                if (historico.getItems() == null || historico.getItems().isEmpty()) {
                    historico.setItems(existente.getItems());
                }
            }

            historicoService.salvar(historico);
            attributes.addFlashAttribute("mensagemSucesso", "Histórico salvo com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/historico/" + historico.getId();
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
    // PDF DO HISTÓRICO ESCOLAR
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
}