package br.com.escola.controller;

import br.com.escola.model.Aluno;
import br.com.escola.model.ResponsavelFinanceiro;
import br.com.escola.service.AlunoService;
import br.com.escola.service.NotaService;
import br.com.escola.service.SerieService;
import br.com.escola.service.TurmaService;
import br.com.escola.service.ResponsavelFinanceiroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

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

        model.addAttribute("alunos", alunos);
        model.addAttribute("series", serieService.listarTodas());
        model.addAttribute("turmas", turmaService.listarTodas());
        model.addAttribute("filtroNome", nome);
        model.addAttribute("filtroSerieId", serieId);
        model.addAttribute("filtroTurmaId", turmaId);
        model.addAttribute("totalAlunos", alunoService.listarTodos().size());
        model.addAttribute("totalFiltrado", alunos.size());

        return "alunos/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Aluno aluno = new Aluno();
        aluno.setResponsavelFinanceiro(new ResponsavelFinanceiro());
        model.addAttribute("aluno", aluno);
        model.addAttribute("series", serieService.listarTodas());
        model.addAttribute("turmas", turmaService.listarTodas());
        return "alunos/cadastrar";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Aluno aluno) {

        ResponsavelFinanceiro respForm = aluno.getResponsavelFinanceiro();

        if (respForm != null && respForm.getNome() != null && !respForm.getNome().isEmpty()) {

            if (respForm.getCpf() != null && !respForm.getCpf().trim().isEmpty()) {
                if (!br.com.escola.util.CpfValidator.isValid(respForm.getCpf())) {
                    return "redirect:/alunos/novo?cpfInvalido";
                }

                String cpfLimpo = respForm.getCpf().replaceAll("[^0-9]", "");

                if (aluno.getId() == null) {
                    if (responsavelService.existeCpf(cpfLimpo)) {
                        return "redirect:/alunos/novo?cpfDuplicado";
                    }
                } else {
                    Aluno existenteAluno = alunoService.buscarPorId(aluno.getId());
                    if (existenteAluno != null && existenteAluno.getResponsavelFinanceiro() != null) {
                        ResponsavelFinanceiro respAtual = responsavelService.buscarPorAlunoId(aluno.getId());
                        if (respAtual != null && !respAtual.getCpf().replaceAll("[^0-9]", "").equals(cpfLimpo)) {
                            if (responsavelService.existeCpf(cpfLimpo)) {
                                return "redirect:/alunos/editar/" + aluno.getId() + "?cpfDuplicado";
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

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        responsavelService.excluirPorAlunoId(id);
        notaService.excluirPorAlunoId(id);
        alunoService.excluir(id);
        return "redirect:/alunos?sucesso";
    }
}
