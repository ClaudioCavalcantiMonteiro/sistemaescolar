package br.com.escola.service;

import br.com.escola.model.Aluno;
import br.com.escola.model.Mensalidade;
import br.com.escola.repository.MensalidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MensalidadeService {

    @Autowired
    private MensalidadeRepository mensalidadeRepository;

    public List<Mensalidade> listarTodas() {
        return mensalidadeRepository.findAll();
    }

    public Mensalidade salvar(Mensalidade m) {
        return mensalidadeRepository.save(m);
    }

    public Mensalidade buscarPorId(Long id) {
        return mensalidadeRepository.findById(id).orElse(null);
    }

    public void excluir(Long id) {
        mensalidadeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Mensalidade> listarPorAluno(Long alunoId) {
        return mensalidadeRepository.findByAlunoWithDetails(alunoId);
    }

    @Transactional(readOnly = true)
    public List<Mensalidade> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return mensalidadeRepository.findByPeriodoWithDetails(inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<Mensalidade> listarPorTurmaEPeriodo(Long turmaId, LocalDate inicio, LocalDate fim) {
        return mensalidadeRepository.findByTurmaAndPeriodo(turmaId, inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<Mensalidade> listarAtrasadas() {
        return mensalidadeRepository.findAtrasadas(LocalDate.now());
    }

    public boolean existeMensalidade(Long alunoId, LocalDate mesReferencia) {
        return mensalidadeRepository.findByAlunoAndMes(alunoId, mesReferencia) != null;
    }

    @Transactional
    public int gerarLote(List<Aluno> alunos, LocalDate mesReferencia, LocalDate dataVencimento,
                         Double valor) {
        int criadas = 0;
        for (Aluno aluno : alunos) {
            if (existeMensalidade(aluno.getId(), mesReferencia)) {
                continue;
            }
            Mensalidade m = new Mensalidade();
            m.setAluno(aluno);
            m.setMesReferencia(mesReferencia);
            m.setDataVencimento(dataVencimento);
            m.setValorOriginal(valor);
            m.setDesconto(0.0);
            m.setJuros(0.0);
            m.setValorPago(0.0);
            m.setStatus("PENDENTE");
            mensalidadeRepository.save(m);
            criadas++;
        }
        return criadas;
    }

    @Transactional
    public void marcarComoPaga(Long id, Double valorPago, LocalDate dataPagamento,
                                String formaPagamento, Double desconto, Double juros,
                                String observacao) {
        Mensalidade m = mensalidadeRepository.findById(id).orElse(null);
        if (m == null) return;

        m.setValorPago(valorPago);
        m.setDataPagamento(dataPagamento != null ? dataPagamento : LocalDate.now());
        m.setFormaPagamento(formaPagamento);
        m.setDesconto(desconto != null ? desconto : 0.0);
        m.setJuros(juros != null ? juros : 0.0);
        m.setObservacao(observacao);
        m.setStatus("PAGA");
        m.setDataAlteracao(LocalDateTime.now());

        mensalidadeRepository.save(m);
    }

    @Transactional
    public void reverterPagamento(Long id) {
        Mensalidade m = mensalidadeRepository.findById(id).orElse(null);
        if (m == null) return;

        m.setValorPago(0.0);
        m.setDataPagamento(null);
        m.setFormaPagamento(null);
        m.setStatus("PENDENTE");
        m.setDataAlteracao(LocalDateTime.now());

        mensalidadeRepository.save(m);
    }

    @Transactional
    public void cancelar(Long id, String motivo) {
        Mensalidade m = mensalidadeRepository.findById(id).orElse(null);
        if (m == null) return;

        m.setStatus("CANCELADA");
        m.setObservacao(motivo);
        m.setDataAlteracao(LocalDateTime.now());

        mensalidadeRepository.save(m);
    }

    @Transactional(readOnly = true)
    public EstatisticasFinanceiras calcularEstatisticas(LocalDate inicio, LocalDate fim) {
        List<Mensalidade> lista = listarPorPeriodo(inicio, fim);

        double totalPrevisto = 0;
        double totalRecebido = 0;
        double totalAtrasado = 0;
        int qtdPagas = 0;
        int qtdPendentes = 0;
        int qtdAtrasadas = 0;

        for (Mensalidade m : lista) {
            totalPrevisto += m.getValorTotal();

            if (m.isPaga()) {
                totalRecebido += m.getValorPago() != null ? m.getValorPago() : 0;
                qtdPagas++;
            } else if (m.isAtrasada()) {
                totalAtrasado += m.getValorTotal();
                qtdAtrasadas++;
            } else if (!m.isCancelada()) {
                qtdPendentes++;
            }
        }

        return new EstatisticasFinanceiras(
                Math.round(totalPrevisto * 100.0) / 100.0,
                Math.round(totalRecebido * 100.0) / 100.0,
                Math.round(totalAtrasado * 100.0) / 100.0,
                qtdPagas, qtdPendentes, qtdAtrasadas, lista.size()
        );
    }

    public static class EstatisticasFinanceiras {
        public final double totalPrevisto;
        public final double totalRecebido;
        public final double totalAtrasado;
        public final int qtdPagas;
        public final int qtdPendentes;
        public final int qtdAtrasadas;
        public final int total;

        public EstatisticasFinanceiras(double totalPrevisto, double totalRecebido, double totalAtrasado,
                                       int qtdPagas, int qtdPendentes, int qtdAtrasadas, int total) {
            this.totalPrevisto = totalPrevisto;
            this.totalRecebido = totalRecebido;
            this.totalAtrasado = totalAtrasado;
            this.qtdPagas = qtdPagas;
            this.qtdPendentes = qtdPendentes;
            this.qtdAtrasadas = qtdAtrasadas;
            this.total = total;
        }
    }
}