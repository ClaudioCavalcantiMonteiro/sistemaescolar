package br.com.escola.controller;

import br.com.escola.model.Aluno;
import br.com.escola.model.Mensalidade;
import br.com.escola.service.AlunoService;
import br.com.escola.service.MensalidadeService;
import br.com.escola.service.TurmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/financeiro")
public class FinanceiroController {

    @Autowired
    private MensalidadeService mensalidadeService;

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private TurmaService turmaService;

    // ========== PÁGINA INICIAL DO MÓDULO ==========
    @GetMapping
    public String index(Model model) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.withDayOfMonth(1);
        LocalDate fim = hoje.withDayOfMonth(hoje.lengthOfMonth());

        MensalidadeService.EstatisticasFinanceiras est =
                mensalidadeService.calcularEstatisticas(inicio, fim);

        model.addAttribute("estatisticas", est);
        model.addAttribute("mesAtual",
                hoje.format(DateTimeFormatter.ofPattern("MMMM/yyyy", new Locale("pt", "BR"))));
        return "financeiro/index";
    }

    // ========== LISTAGEM ==========
    @GetMapping("/mensalidades")
    public String listar(@RequestParam(required = false) String status,
                         @RequestParam(required = false) Long turmaId,
                         @RequestParam(required = false) Integer mes,
                         @RequestParam(required = false) Integer ano,
                         Model model) {

        LocalDate hoje = LocalDate.now();
        if (mes == null) mes = hoje.getMonthValue();
        if (ano == null) ano = hoje.getYear();

        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        List<Mensalidade> mensalidades;
        if (turmaId != null && turmaId > 0) {
            mensalidades = mensalidadeService.listarPorTurmaEPeriodo(turmaId, inicio, fim);
        } else {
            mensalidades = mensalidadeService.listarPorPeriodo(inicio, fim);
        }

        if (status != null && !status.isEmpty()) {
            final String s = status;
            mensalidades = mensalidades.stream()
                    .filter(m -> {
                        if ("ATRASADA".equals(s)) return m.isAtrasada();
                        return s.equalsIgnoreCase(m.getStatus());
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        model.addAttribute("mensalidades", mensalidades);
        model.addAttribute("turmas", turmaService.listarTodas());
        model.addAttribute("mes", mes);
        model.addAttribute("ano", ano);
        model.addAttribute("filtroStatus", status);
        model.addAttribute("filtroTurmaId", turmaId);

        MensalidadeService.EstatisticasFinanceiras est =
                mensalidadeService.calcularEstatisticas(inicio, fim);
        model.addAttribute("estatisticas", est);

        String[] meses = {"", "Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                          "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
        model.addAttribute("nomeMes", meses[mes]);

        return "financeiro/mensalidades";
    }

    // ========== CADASTRAR INDIVIDUAL ==========
    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("alunos", alunoService.listarTodos());
        model.addAttribute("mensalidade", new Mensalidade());
        return "financeiro/cadastrar";
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam Long alunoId,
                         @RequestParam String mesReferencia,  // formato "yyyy-MM"
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimento,
                         @RequestParam Double valorOriginal,
                         @RequestParam(required = false) String observacao) {

        // Converte "yyyy-MM" para o primeiro dia do mês
        LocalDate mes = LocalDate.parse(mesReferencia + "-01");

        if (mensalidadeService.existeMensalidade(alunoId, mes)) {
            return "redirect:/financeiro/mensalidades?erroDuplicado";
        }

        Aluno aluno = alunoService.buscarPorId(alunoId);
        if (aluno == null) {
            return "redirect:/financeiro/mensalidades";
        }

        Mensalidade m = new Mensalidade();
        m.setAluno(aluno);
        m.setMesReferencia(mes);
        m.setDataVencimento(dataVencimento);
        m.setValorOriginal(valorOriginal);
        m.setObservacao(observacao);
        mensalidadeService.salvar(m);

        return "redirect:/financeiro/mensalidades?sucesso";
    }

    // ========== GERAR EM LOTE ==========
    @GetMapping("/gerar-lote")
    public String gerarLoteForm(Model model) {
        model.addAttribute("turmas", turmaService.listarTodas());
        return "financeiro/gerar-lote";
    }

    @PostMapping("/gerar-lote")
    public String gerarLote(@RequestParam Long turmaId,
                            @RequestParam String mesReferencia,  // formato "yyyy-MM"
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimento,
                            @RequestParam Double valor) {

        // Converte "yyyy-MM" para o primeiro dia do mês
        LocalDate mes = LocalDate.parse(mesReferencia + "-01");

        List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);
        int criadas = mensalidadeService.gerarLote(alunos, mes, dataVencimento, valor);

        return "redirect:/financeiro/mensalidades?loteSucesso=" + criadas;
    }

    // ========== MARCAR COMO PAGA ==========
    @GetMapping("/pagar/{id}")
    public String pagarForm(@PathVariable Long id, Model model) {
        Mensalidade m = mensalidadeService.buscarPorId(id);
        if (m == null) {
            return "redirect:/financeiro/mensalidades";
        }
        model.addAttribute("mensalidade", m);
        return "financeiro/pagar";
    }

    @PostMapping("/pagar")
    public String pagar(@RequestParam Long id,
                        @RequestParam Double valorPago,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPagamento,
                        @RequestParam String formaPagamento,
                        @RequestParam(required = false) Double desconto,
                        @RequestParam(required = false) Double juros,
                        @RequestParam(required = false) String observacao) {

        mensalidadeService.marcarComoPaga(id, valorPago, dataPagamento, formaPagamento,
                desconto, juros, observacao);
        return "redirect:/financeiro/recibo/" + id;
    }

    // ========== REVERTER PAGAMENTO ==========
    @GetMapping("/reverter/{id}")
    public String reverter(@PathVariable Long id) {
        mensalidadeService.reverterPagamento(id);
        return "redirect:/financeiro/mensalidades?revertido";
    }

    // ========== CANCELAR ==========
    @GetMapping("/cancelar/{id}")
    public String cancelar(@PathVariable Long id) {
        mensalidadeService.cancelar(id, "Cancelada pelo usuário");
        return "redirect:/financeiro/mensalidades?cancelada";
    }

    // ========== RECIBO ==========
    @GetMapping("/recibo/{id}")
    public String recibo(@PathVariable Long id, Model model) {
        Mensalidade m = mensalidadeService.buscarPorId(id);
        if (m == null) {
            return "redirect:/financeiro/mensalidades";
        }
        model.addAttribute("mensalidade", m);
        model.addAttribute("aluno", m.getAluno());
        return "financeiro/recibo";
    }

    // ========== RELATÓRIO ==========
    @GetMapping("/relatorio")
    public String relatorio(@RequestParam(required = false) Long turmaId,
                            @RequestParam(required = false) Integer mes,
                            @RequestParam(required = false) Integer ano,
                            Model model) {
        LocalDate hoje = LocalDate.now();
        if (mes == null) mes = hoje.getMonthValue();
        if (ano == null) ano = hoje.getYear();

        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        List<Mensalidade> lista;
        if (turmaId != null && turmaId > 0) {
            lista = mensalidadeService.listarPorTurmaEPeriodo(turmaId, inicio, fim);
        } else {
            lista = mensalidadeService.listarPorPeriodo(inicio, fim);
        }

        MensalidadeService.EstatisticasFinanceiras est =
                mensalidadeService.calcularEstatisticas(inicio, fim);

        String[] meses = {"", "Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                          "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};

        model.addAttribute("mensalidades", lista);
        model.addAttribute("estatisticas", est);
        model.addAttribute("turmas", turmaService.listarTodas());
        model.addAttribute("turmaId", turmaId);
        model.addAttribute("mes", mes);
        model.addAttribute("ano", ano);
        model.addAttribute("nomeMes", meses[mes]);
        return "financeiro/relatorio";
    }
}