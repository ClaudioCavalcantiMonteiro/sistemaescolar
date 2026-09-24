package br.com.escola.service;

import br.com.escola.model.Aluno;
import br.com.escola.model.Escola;
import br.com.escola.model.Frequencia;
import br.com.escola.model.HistoricoEscolar;
import br.com.escola.model.ItemHistorico;
import br.com.escola.model.Materia;
import br.com.escola.model.Nota;
import br.com.escola.repository.EscolaRepository;
import br.com.escola.repository.HistoricoEscolarRepository;
import br.com.escola.repository.ItemHistoricoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HistoricoEscolarService {

    @Autowired
    private HistoricoEscolarRepository historicoRepository;

    @Autowired
    private ItemHistoricoRepository itemRepository;

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private NotaService notaService;

    @Autowired
    private FrequenciaService frequenciaService;

    @Autowired
    private EscolaRepository escolaRepository;

    private static final double MEDIA_PADRAO = 6.0;
    private static final String REGRA_PADRAO = "DEPENDENCIA";
    private static final int MAX_DEPENDENCIA_PADRAO = 2;

    // ==================================================================
    // LISTAGEM / BUSCA
    // ==================================================================
    public List<HistoricoEscolar> listarTodos() {
        return historicoRepository.findAllWithItems();
    }

    public List<HistoricoEscolar> listarPorAluno(Long alunoId) {
        return historicoRepository.findByAlunoIdWithItems(alunoId);
    }

    public HistoricoEscolar buscarPorId(Long id) {
        Optional<HistoricoEscolar> opt = historicoRepository.findByIdWithItems(id);
        return opt.orElse(null);
    }

    public boolean existeHistorico(Long alunoId, Integer anoLetivo) {
        return historicoRepository.existsByAlunoIdAndAnoLetivo(alunoId, anoLetivo);
    }

    // ==================================================================
    // CÁLCULO DA SITUAÇÃO FINAL
    // ==================================================================
    private String calcularSituacaoFinal(HistoricoEscolar historico, Escola escola) {
        int total = historico.getTotalDisciplinas();
        int reprovadas = historico.getTotalReprovadas();
        int anoAtual = LocalDate.now().getYear();
        Integer anoLetivo = historico.getAnoLetivo();

        if (total == 0 || (anoLetivo != null && anoLetivo >= anoAtual)) {
            return "CURSANDO";
        }

        if (reprovadas == 0) {
            return "APROVADO";
        }

        String regra = (escola != null) ? escola.getRegraAprovacaoSegura() : REGRA_PADRAO;
        int maxDependencia = (escola != null) ? escola.getMaxMateriasDependenciaSeguro() : MAX_DEPENDENCIA_PADRAO;
        double mediaMin = (escola != null) ? escola.getMediaAprovacaoSegura() : MEDIA_PADRAO;

        switch (regra.toUpperCase()) {
            case "RIGIDA":
                return "REPROVADO";
            case "MEDIA_GERAL":
                return (historico.getMediaGeral() >= mediaMin) ? "APROVADO" : "REPROVADO";
            case "DEPENDENCIA":
            default:
                if (reprovadas <= maxDependencia) {
                    return "APROVADO_COM_DEPENDENCIA";
                }
                return "REPROVADO";
        }
    }

    // ==================================================================
    // GERAR HISTORICO AUTOMATICAMENTE
    // ==================================================================
    @Transactional
    public HistoricoEscolar gerarHistorico(Long alunoId, Integer anoLetivo) {

        Aluno aluno = alunoService.buscarPorId(alunoId);
        if (aluno == null) {
            throw new RuntimeException("Aluno não encontrado: " + alunoId);
        }

        if (historicoRepository.existsByAlunoIdAndAnoLetivo(alunoId, anoLetivo)) {
            throw new RuntimeException("Já existe um histórico para este aluno no ano " + anoLetivo);
        }

        HistoricoEscolar historico = new HistoricoEscolar();
        historico.setAluno(aluno);

        List<Escola> escolas = escolaRepository.findAll();
        Escola escola = escolas.isEmpty() ? null : escolas.get(0);
        historico.setEscola(escola);

        double mediaAprovacao = (escola != null) ? escola.getMediaAprovacaoSegura() : MEDIA_PADRAO;
        historico.setMediaAprovacao(mediaAprovacao);

        historico.setAnoLetivo(anoLetivo);
        historico.setSerie(aluno.getSerie() != null ? aluno.getSerie().getNome() : "-");
        historico.setTurma(aluno.getTurma() != null ? aluno.getTurma().getNome() : "-");
        historico.setTurno(aluno.getTurma() != null && aluno.getTurma().getTurno() != null
                ? aluno.getTurma().getTurno() : "-");

        List<Nota> notas = notaService.listarPorAlunoEAno(alunoId, anoLetivo);
        List<Frequencia> frequencias = frequenciaService.listarPorAluno(alunoId);

        Map<Long, List<Nota>> notasPorMateria = new HashMap<>();
        for (Nota n : notas) {
            if (n.getMateria() == null) continue;
            notasPorMateria.computeIfAbsent(n.getMateria().getId(), k -> new ArrayList<>()).add(n);
        }

        Map<Long, Integer> faltasPorMateria = new HashMap<>();
        for (Frequencia f : frequencias) {
            if (Boolean.FALSE.equals(f.getPresente()) && f.getMateria() != null) {
                faltasPorMateria.merge(f.getMateria().getId(), 1, Integer::sum);
            }
        }

        int ordem = 1;
        for (Map.Entry<Long, List<Nota>> entry : notasPorMateria.entrySet()) {
            List<Nota> notasMateria = entry.getValue();
            if (notasMateria.isEmpty()) continue;

            Materia materia = notasMateria.get(0).getMateria();

            double somaMedias = 0;
            int countMedias = 0;
            for (Nota n : notasMateria) {
                if (n.getMediaFinal() != null) {
                    somaMedias += n.getMediaFinal();
                    countMedias++;
                }
            }
            double mediaFinal = countMedias > 0 ? somaMedias / countMedias : 0.0;
            mediaFinal = Math.round(mediaFinal * 100.0) / 100.0;

            ItemHistorico item = new ItemHistorico();
            item.setMateria(materia);
            item.setNomeMateria(materia.getNome());
            item.setNotaFinal(mediaFinal);
            item.setFaltas(faltasPorMateria.getOrDefault(materia.getId(), 0));
            item.setCargaHoraria(80);
            item.setResultado(mediaFinal >= mediaAprovacao ? "APROVADO" : "REPROVADO");
            item.setOrdem(ordem++);

            historico.addItem(item);
        }

        historico.setSituacaoFinal(calcularSituacaoFinal(historico, escola));

        int cargaTotal = historico.getItems().stream()
                .mapToInt(i -> i.getCargaHoraria() != null ? i.getCargaHoraria() : 0)
                .sum();
        historico.setCargaHorariaTotal(cargaTotal);
        historico.setDiasLetivos(200);

        return historicoRepository.save(historico);
    }

    // ==================================================================
    // GERAR HISTORICO EXTERNO (transferência)
    // ==================================================================
    @Transactional
    public HistoricoEscolar gerarHistoricoExterno(Long alunoId, Integer anoLetivo,
                                                    String escolaOrigem, String cidadeOrigem) {

        Aluno aluno = alunoService.buscarPorId(alunoId);
        if (aluno == null) {
            throw new RuntimeException("Aluno não encontrado: " + alunoId);
        }

        if (historicoRepository.existsByAlunoIdAndAnoLetivo(alunoId, anoLetivo)) {
            throw new RuntimeException("Já existe um histórico para este aluno no ano " + anoLetivo);
        }

        HistoricoEscolar historico = new HistoricoEscolar();
        historico.setAluno(aluno);

        List<Escola> escolas = escolaRepository.findAll();
        Escola escola = escolas.isEmpty() ? null : escolas.get(0);
        historico.setEscola(escola);

        double mediaAprovacao = (escola != null) ? escola.getMediaAprovacaoSegura() : MEDIA_PADRAO;
        historico.setMediaAprovacao(mediaAprovacao);

        historico.setEscolaOrigem(escolaOrigem);
        historico.setCidadeOrigem(cidadeOrigem);

        historico.setAnoLetivo(anoLetivo);
        historico.setSerie("-");
        historico.setTurma("-");
        historico.setTurno("-");

        historico.setSituacaoFinal("CURSANDO");
        historico.setCargaHorariaTotal(0);
        historico.setDiasLetivos(200);
        historico.setObservacoes("Histórico de transferência — preencher com os dados da escola de origem.");

        return historicoRepository.save(historico);
    }

    // ==================================================================
    // SALVAR
    // ==================================================================
    @Transactional
    public HistoricoEscolar salvar(HistoricoEscolar historico) {
        double media = historico.getMediaAprovacaoSegura();

        if (historico.getItems() != null) {
            int ordem = 1;
            for (ItemHistorico item : historico.getItems()) {
                item.setHistorico(historico);
                if (item.getOrdem() == null) {
                    item.setOrdem(ordem);
                }
                ordem++;

                double nota = item.getNotaFinal() != null ? item.getNotaFinal() : 0.0;
                if (!"CURSANDO".equalsIgnoreCase(item.getResultado())) {
                    item.setResultado(nota >= media ? "APROVADO" : "REPROVADO");
                }
            }
        }

        int cargaTotal = historico.getItems().stream()
                .mapToInt(i -> i.getCargaHoraria() != null ? i.getCargaHoraria() : 0)
                .sum();
        historico.setCargaHorariaTotal(cargaTotal);

        return historicoRepository.save(historico);
    }

    // ==================================================================
    // MARCAR COMO EMITIDO
    // ==================================================================
    @Transactional
    public void marcarComoEmitido(Long id) {
        HistoricoEscolar h = historicoRepository.findById(id).orElse(null);
        if (h != null) {
            h.setEmitido(true);
            h.setDataEmissao(LocalDateTime.now());
            historicoRepository.save(h);
        }
    }

    // ==================================================================
    // EXCLUIR
    // ==================================================================
    @Transactional
    public void excluir(Long id) {
        itemRepository.deleteByHistoricoId(id);
        historicoRepository.deleteById(id);
    }

    // ==================================================================
    // RECALCULAR
    // ==================================================================
    @Transactional
    public HistoricoEscolar recalcular(Long id) {
        HistoricoEscolar historico = buscarPorId(id);
        if (historico == null) {
            throw new RuntimeException("Histórico não encontrado: " + id);
        }

        if (historico.isExterno()) {
            throw new RuntimeException("Este histórico é de escola externa (transferência). Edite manualmente.");
        }

        Long alunoId = historico.getAluno().getId();
        Integer anoLetivo = historico.getAnoLetivo();
        double media = historico.getMediaAprovacaoSegura();

        List<Nota> notas = notaService.listarPorAlunoEAno(alunoId, anoLetivo);
        List<Frequencia> frequencias = frequenciaService.listarPorAluno(alunoId);

        Map<Long, Integer> faltasPorMateria = new HashMap<>();
        for (Frequencia f : frequencias) {
            if (Boolean.FALSE.equals(f.getPresente()) && f.getMateria() != null) {
                faltasPorMateria.merge(f.getMateria().getId(), 1, Integer::sum);
            }
        }

        for (ItemHistorico item : historico.getItems()) {
            if (item.getMateria() == null) continue;

            double somaMedias = 0;
            int count = 0;
            for (Nota n : notas) {
                if (n.getMateria() != null
                        && n.getMateria().getId().equals(item.getMateria().getId())
                        && n.getMediaFinal() != null) {
                    somaMedias += n.getMediaFinal();
                    count++;
                }
            }
            double mediaFinal = count > 0 ? somaMedias / count : 0.0;
            mediaFinal = Math.round(mediaFinal * 100.0) / 100.0;

            item.setNotaFinal(mediaFinal);
            item.setFaltas(faltasPorMateria.getOrDefault(item.getMateria().getId(), 0));
            item.setResultado(mediaFinal >= media ? "APROVADO" : "REPROVADO");
        }

        Escola escola = historico.getEscola();
        if (escola == null) {
            List<Escola> escolas = escolaRepository.findAll();
            escola = escolas.isEmpty() ? null : escolas.get(0);
        }
        historico.setSituacaoFinal(calcularSituacaoFinal(historico, escola));

        int cargaTotal = historico.getItems().stream()
                .mapToInt(i -> i.getCargaHoraria() != null ? i.getCargaHoraria() : 0)
                .sum();
        historico.setCargaHorariaTotal(cargaTotal);

        return historicoRepository.save(historico);
    }

    // ==================================================================
    // ADICIONAR ITEM MANUALMENTE
    // ==================================================================
    @Transactional
    public ItemHistorico adicionarItem(Long historicoId) {
        HistoricoEscolar historico = buscarPorId(historicoId);
        if (historico == null) {
            throw new RuntimeException("Histórico não encontrado: " + historicoId);
        }

        ItemHistorico item = new ItemHistorico();
        item.setHistorico(historico);
        item.setNomeMateria("Nova matéria");
        item.setNotaFinal(0.0);
        item.setFaltas(0);
        item.setCargaHoraria(80);
        item.setResultado("CURSANDO");
        item.setOrdem(historico.getItems().size() + 1);

        return itemRepository.save(item);
    }

    // ==================================================================
    // REMOVER ITEM
    // ==================================================================
    @Transactional
    public void removerItem(Long itemId) {
        ItemHistorico item = itemRepository.findById(itemId).orElse(null);
        if (item != null) {
            itemRepository.delete(item);
        }
    }
}