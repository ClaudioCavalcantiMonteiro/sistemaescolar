package br.com.escola.controller;

import br.com.escola.model.Turma;
import br.com.escola.service.PdfService;
import br.com.escola.service.SerieService;
import br.com.escola.service.TurmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/turmas")
public class TurmaController {

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private SerieService serieService;

    @Autowired
    private PdfService pdfService;

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
    public String salvar(@ModelAttribute Turma turma, RedirectAttributes attributes) {
        try {
            turmaService.salvar(turma);
            attributes.addFlashAttribute("mensagemSucesso", "Turma salva com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao salvar a turma: " + e.getMessage());
        }
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
    public String excluir(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            turmaService.excluir(id);
            attributes.addFlashAttribute("mensagemSucesso", "Turma excluída com sucesso!");
        } catch (DataIntegrityViolationException e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Não é possível excluir esta turma, pois existem alunos vinculados.");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao excluir: " + e.getMessage());
        }
        return "redirect:/turmas";
    }

    @GetMapping("/alunos")
    public String listarAlunosPorTurma(Model model) {
        model.addAttribute("turmas", turmaService.listarTodasComAlunos());
        return "turmas/alunos-por-turma";
    }

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

    // ==================================================================
    // RELATORIO DE ALUNOS POR TURMA (PDF) - CORRIGIDO com byte[]
    // ==================================================================
    @GetMapping("/{id}/alunos/pdf")
    public ResponseEntity<byte[]> relatorioAlunosPorTurma(@PathVariable Long id) {
        try {
            Turma turma = turmaService.buscarPorId(id);
            if (turma == null) {
                System.err.println("[PDF] Turma nao encontrada: " + id);
                return ResponseEntity.notFound().build();
            }

            String nomeArquivo = "alunos_turma_" +
                    turma.getNome().replaceAll("\\s+", "_") + ".pdf";

            System.out.println("[PDF] Gerando relatorio para turma: " + turma.getNome());

            ByteArrayInputStream pdfStream = pdfService.gerarRelatorioAlunosPorTurma(id);
            byte[] bytes = pdfStream.readAllBytes();

            System.out.println("[PDF] Tamanho do PDF gerado: " + bytes.length + " bytes");

            if (bytes.length == 0) {
                System.err.println("[PDF] ERRO: PDF gerado esta vazio!");
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
            System.err.println("[PDF] ERRO ao gerar PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}