package br.com.escola.controller;

import br.com.escola.util.CpfValidator;
import br.com.escola.model.Aluno;
import br.com.escola.model.ResponsavelFinanceiro;
import br.com.escola.service.AlunoService;
import br.com.escola.service.NotaService;
import br.com.escola.service.SerieService;
import br.com.escola.service.TurmaService;
import br.com.escola.service.ResponsavelFinanceiroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Controller
@RequestMapping("/alunos")
public class AlunoController {

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private SerieService serieService;

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private ResponsavelFinanceiroService responsavelService;

    @Autowired
    private NotaService notaService;

    @GetMapping
    public String listar(@RequestParam(required = false) String nome,
                         @RequestParam(required = false) Long serieId,
                         @RequestParam(required = false) Long turmaId,
                         @RequestParam(required = false) Integer anoLetivo,
                         Model model) {

        List<Aluno> alunos = alunoService.listarTodos();

        if (nome != null && !nome.trim().isEmpty()) {
            String termo = nome.trim().toLowerCase();
            alunos = alunos.stream()
                    .filter(a -> a.getNome() != null && a.getNome().toLowerCase().contains(termo))
                    .collect(Collectors.toList());
        }
        if (serieId != null && serieId > 0) {
            alunos = alunos.stream()
                    .filter(a -> a.getSerie() != null && a.getSerie().getId().equals(serieId))
                    .collect(Collectors.toList());
        }
        if (turmaId != null && turmaId > 0) {
            alunos = alunos.stream()
                    .filter(a -> a.getTurma() != null && a.getTurma().getId().equals(turmaId))
                    .collect(Collectors.toList());
        }
        if (anoLetivo != null && anoLetivo > 0) {
            alunos = alunos.stream()
                    .filter(a -> a.getAnoLetivo() != null && a.getAnoLetivo().equals(anoLetivo))
                    .collect(Collectors.toList());
        }

        model.addAttribute("alunos", alunos);
        model.addAttribute("series", serieService.listarTodas());
        model.addAttribute("turmas", turmaService.listarTodas());
        model.addAttribute("filtroNome", nome);
        model.addAttribute("filtroSerieId", serieId);
        model.addAttribute("filtroTurmaId", turmaId);
        model.addAttribute("filtroAnoLetivo", anoLetivo);
        model.addAttribute("anoAtual", LocalDate.now().getYear());
        model.addAttribute("totalAlunos", alunoService.listarTodos().size());
        model.addAttribute("totalFiltrado", alunos.size());

        return "alunos/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Aluno aluno = new Aluno();
        aluno.setResponsavelFinanceiro(new ResponsavelFinanceiro());
        aluno.setAnoLetivo(LocalDate.now().getYear());
        aluno.setDataMatricula(LocalDate.now());
        aluno.setMatriculaAtiva(true);
        model.addAttribute("aluno", aluno);
        model.addAttribute("series", serieService.listarTodas());
        model.addAttribute("turmas", turmaService.listarTodas());
        return "alunos/cadastrar";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Aluno aluno, Model model) {

        ResponsavelFinanceiro respForm = aluno.getResponsavelFinanceiro();

        if (respForm != null && respForm.getNome() != null && !respForm.getNome().isEmpty()) {

            if (respForm.getCpf() != null && !respForm.getCpf().trim().isEmpty()) {

                if (!CpfValidator.isValid(respForm.getCpf())) {
                    model.addAttribute("aluno", aluno);
                    model.addAttribute("series", serieService.listarTodas());
                    model.addAttribute("turmas", turmaService.listarTodas());
                    model.addAttribute("cpfInvalido", true);
                    return "alunos/cadastrar";
                }

                String cpfLimpo = respForm.getCpf().replaceAll("[^0-9]", "");

                if (aluno.getId() == null) {
                    if (responsavelService.existeCpf(cpfLimpo)) {
                        model.addAttribute("aluno", aluno);
                        model.addAttribute("series", serieService.listarTodas());
                        model.addAttribute("turmas", turmaService.listarTodas());
                        model.addAttribute("cpfDuplicado", true);
                        return "alunos/cadastrar";
                    }
                } else {
                    Aluno existenteAluno = alunoService.buscarPorId(aluno.getId());
                    if (existenteAluno != null && existenteAluno.getResponsavelFinanceiro() != null) {
                        ResponsavelFinanceiro respAtual = responsavelService.buscarPorAlunoId(aluno.getId());
                        if (respAtual != null && !respAtual.getCpf().replaceAll("[^0-9]", "").equals(cpfLimpo)) {
                            if (responsavelService.existeCpf(cpfLimpo)) {
                                model.addAttribute("aluno", aluno);
                                model.addAttribute("series", serieService.listarTodas());
                                model.addAttribute("turmas", turmaService.listarTodas());
                                model.addAttribute("cpfDuplicado", true);
                                return "alunos/cadastrar";
                            }
                        }
                    }
                }
            }
        }

        if (aluno.getId() != null) {
            Aluno existente = alunoService.buscarPorId(aluno.getId());
            if (existente != null) {
                existente.setNome(aluno.getNome());
                existente.setDataNascimento(aluno.getDataNascimento());
                existente.setMatricula(aluno.getMatricula());
                existente.setSerie(aluno.getSerie());
                existente.setTurma(aluno.getTurma());
                existente.setAnoLetivo(aluno.getAnoLetivo());
                existente.setDataMatricula(aluno.getDataMatricula());
                existente.setMatriculaAtiva(aluno.getMatriculaAtiva());

                existente.setNaturalidade(aluno.getNaturalidade());
                existente.setNacionalidade(aluno.getNacionalidade());
                existente.setSexo(aluno.getSexo());
                existente.setNomePai(aluno.getNomePai());
                existente.setNomeMae(aluno.getNomeMae());
                existente.setCpfAluno(aluno.getCpfAluno());
                existente.setRgAluno(aluno.getRgAluno());
                existente.setTelefone(aluno.getTelefone());

                if (respForm != null && respForm.getNome() != null && !respForm.getNome().isEmpty()) {
                    ResponsavelFinanceiro respExistente = responsavelService.buscarPorAlunoId(existente.getId());
                    if (respExistente != null) {
                        respExistente.setNome(respForm.getNome());
                        respExistente.setGrauParentesco(respForm.getGrauParentesco());
                        respExistente.setEndereco(respForm.getEndereco());
                        respExistente.setNumero(respForm.getNumero());
                        respExistente.setBairro(respForm.getBairro());
                        respExistente.setCep(respForm.getCep());
                        respExistente.setCidade(respForm.getCidade());
                        respExistente.setCpf(respForm.getCpf());
                        respExistente.setRg(respForm.getRg());
                        respExistente.setTelefone(respForm.getTelefone());
                        respExistente.setCelular(respForm.getCelular());
                        respExistente.setEmail(respForm.getEmail());
                        responsavelService.salvar(respExistente);
                    } else {
                        respForm.setAluno(existente);
                        responsavelService.salvar(respForm);
                    }
                }
                aluno = existente;
            }
        } else {
            if (respForm != null && respForm.getNome() != null && !respForm.getNome().isEmpty()) {
                respForm.setAluno(aluno);
            }
        }

        alunoService.salvar(aluno);
        return "redirect:/alunos?sucesso";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Aluno aluno = alunoService.buscarPorId(id);
        if (aluno.getResponsavelFinanceiro() == null) {
            aluno.setResponsavelFinanceiro(new ResponsavelFinanceiro());
        }
        model.addAttribute("aluno", aluno);
        model.addAttribute("series", serieService.listarTodas());
        model.addAttribute("turmas", turmaService.listarTodas());
        return "alunos/cadastrar";
    }

    // ==================================================================
    // EXCLUIR — com debug de log no console
    // ==================================================================
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            System.out.println(">>> [EXCLUIR] Tentando excluir aluno ID: " + id);

            Aluno aluno = alunoService.buscarPorId(id);
            if (aluno == null) {
                attributes.addFlashAttribute("mensagemErro", "Aluno não encontrado.");
                return "redirect:/alunos";
            }

            System.out.println(">>> [EXCLUIR] Aluno encontrado: " + aluno.getNome());

            // 1) Tenta excluir responsável (não deve dar erro se existir FK)
            try {
                responsavelService.excluirPorAlunoId(id);
                System.out.println(">>> [EXCLUIR] Responsável excluído (ou não existia).");
            } catch (Exception e) {
                System.err.println(">>> [EXCLUIR] Erro ao excluir responsável: " + e.getMessage());
                // não interrompe, segue para o resto
            }

            // 2) Tenta excluir notas (não deve dar erro se existir FK)
            try {
                notaService.excluirPorAlunoId(id);
                System.out.println(">>> [EXCLUIR] Notas excluídas (ou não existiam).");
            } catch (Exception e) {
                System.err.println(">>> [EXCLUIR] Erro ao excluir notas: " + e.getMessage());
                // não interrompe
            }

            // 3) Tenta excluir o aluno (aqui pode dar FK de mensalidade/frequência/histórico)
            alunoService.excluir(id);
            System.out.println(">>> [EXCLUIR] Aluno excluído com sucesso!");

            attributes.addFlashAttribute("mensagemSucesso", "Aluno excluído com sucesso!");

        } catch (DataIntegrityViolationException e) {
            System.err.println(">>> [EXCLUIR] DataIntegrityViolationException: " + e.getMessage());
            attributes.addFlashAttribute("mensagemErro",
                    "Não é possível excluir este aluno pois ele possui mensalidades, notas, frequências ou históricos vinculados. " +
                    "Use a opção 'Desativar Matrícula' para preservar o histórico.");
        } catch (Exception e) {
            System.err.println(">>> [EXCLUIR] Exception genérica: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            attributes.addFlashAttribute("mensagemErro",
                    "Erro ao excluir o aluno: " + e.getMessage());
        }
        return "redirect:/alunos";
    }

    // ===== REMATRICULAR =====
    @GetMapping("/rematricular/{id}")
    public String rematricular(@PathVariable Long id) {
        Aluno aluno = alunoService.buscarPorId(id);
        if (aluno != null) {
            int anoAtual = aluno.getAnoLetivo() != null ? aluno.getAnoLetivo() : LocalDate.now().getYear();
            aluno.setAnoLetivo(anoAtual + 1);
            aluno.setDataMatricula(LocalDate.now());
            aluno.setMatriculaAtiva(true);
            alunoService.salvar(aluno);
            return "redirect:/alunos?rematriculado=" + aluno.getNome();
        }
        return "redirect:/alunos";
    }

    // ===== DESATIVAR MATRÍCULA =====
    @GetMapping("/desativar/{id}")
    public String desativar(@PathVariable Long id) {
        Aluno aluno = alunoService.buscarPorId(id);
        if (aluno != null) {
            aluno.setMatriculaAtiva(false);
            alunoService.salvar(aluno);
        }
        return "redirect:/alunos?desativado";
    }

    // ===== ATIVAR MATRÍCULA =====
    @GetMapping("/ativar/{id}")
    public String ativar(@PathVariable Long id) {
        Aluno aluno = alunoService.buscarPorId(id);
        if (aluno != null) {
            aluno.setMatriculaAtiva(true);
            alunoService.salvar(aluno);
        }
        return "redirect:/alunos?ativado";
    }
}