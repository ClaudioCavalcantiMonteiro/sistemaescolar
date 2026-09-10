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
import org.springframework.web.bind.annotation.*;

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
    public String listar(Model model) {
        model.addAttribute("alunos", alunoService.listarTodos());
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
        // Se for edição, busca o existente e atualiza
        if (aluno.getId() != null) {
            Aluno existente = alunoService.buscarPorId(aluno.getId());
            if (existente != null) {
                existente.setNome(aluno.getNome());
                existente.setDataNascimento(aluno.getDataNascimento());
                existente.setMatricula(aluno.getMatricula());
                existente.setSerie(aluno.getSerie());
                existente.setTurma(aluno.getTurma());

                // -- CORREÇÃO: atualiza o responsável a partir do objeto aluno --
                ResponsavelFinanceiro respForm = aluno.getResponsavelFinanceiro();
                if (respForm != null && respForm.getNome() != null && !respForm.getNome().isEmpty()) {
                    ResponsavelFinanceiro respExistente = responsavelService.buscarPorAlunoId(existente.getId());
                    if (respExistente != null) {
                        // Atualiza o existente
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
                        // Cria novo responsável
                        respForm.setAluno(existente);
                        responsavelService.salvar(respForm);
                    }
                } else {
                    // Se o responsável foi removido (nome vazio), apaga o existente
                    ResponsavelFinanceiro respExistente = responsavelService.buscarPorAlunoId(existente.getId());
                    if (respExistente != null) {
                        responsavelService.excluir(respExistente.getId());
                    }
                }

                aluno = existente; // usa o existente para salvar
            }
        } else {
            // Novo aluno: vincula o responsável se houver
            if (aluno.getResponsavelFinanceiro() != null && aluno.getResponsavelFinanceiro().getNome() != null
                    && !aluno.getResponsavelFinanceiro().getNome().isEmpty()) {
                aluno.getResponsavelFinanceiro().setAluno(aluno);
            }
        }

        alunoService.salvar(aluno);
        return "redirect:/alunos";
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
        return "redirect:/alunos";
    }
}