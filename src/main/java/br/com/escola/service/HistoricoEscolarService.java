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
    // GERAR HISTORICO AUTOMATICAMENTE
    // Usa apenas notas do ANO LETIVO específico
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

        // ===== 1. Dados do cabeçalho =====
        HistoricoEscolar historico = new HistoricoEscolar();
        historico.setAluno(aluno);

        List<Escola> escolas = escolaRepository.findAll();
        Escola escola = escolas.isEmpty() ? null : escolas.get(0);
        historico.setEscola(escola);

        // Média da escola (com fallback para 6.0)
        double mediaAprovacao = (escola != null && escola.getMediaAprovacao() != null)
                ? escola.getMediaAprovacao()
                : MEDIA_PADRAO;
        historico.setMediaAprovacao(mediaAprovacao);

        historico.setAnoLetivo(anoLetivo);
        historico.setSerie(aluno.getSerie() != null ? aluno.getSerie().getNome() : "-");
        historico.setTurma(aluno.getTurma() != null ? aluno.getTurma().getNome() : "-");
        historico.setTurno(aluno.getTurma() != null && aluno.getTurma().getTurno() != null
                ? aluno.getTurma().getTurno() : "-");

        // ===== 2. Buscar notas DO ANO LETIVO e frequências =====
        // ⚠️ IMPORTANTE: notas filtradas por ano
        List<Nota> notas = notaService.listarPorAlunoEAno(alunoId, anoLetivo);
        // Frequências ainda não têm anoLetivo — pegamos todas do aluno
        List<Frequencia> frequencias = frequenciaService.listarPorAluno(alunoId);

        // ===== 3. Agrupar notas por matéria =====
        Map<Long, List<Nota>> notasPorMateria = new HashMap<>();
        for (Nota n : notas) {
            if (n.getMateria() == null) continue;
            notasPorMateria.computeIfAbsent(n.getMateria().getId(), k -> new ArrayList<>()).add(n);
        }

        // ===== 4. Contar faltas por matéria =====
        Map<Long, Integer> faltasPorMateria = new HashMap<>();
        for (Frequencia f : frequencias) {
            if (Boolean.FALSE.equals(f.getPresente()) && f.getMateria() != null) {
                faltasPorMateria.merge(f.getMateria().getId(), 1, Integer::sum);
            }
        }

        // ===== 5. Criar um item por matéria =====
        int ordem = 1;
        for (Map.Entry<Long, List<Nota>> entry : notasPorMateria.entrySet()) {
            List<Nota> notasMateria = entry.getValue();
            if (notasMateria.isEmpty()) continue;

            Materia materia = notasMateria.get(0).getMateria();

            // 5.1 — Média final (média das médias de cada unidade)
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

            // 5.2 — Criar item
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

        // ===== 6. Situação final geral =====
        int total = historico.getItems().size();
        int aprovadas = historico.getTotalAprovadas();
        int anoAtual = LocalDate.now().getYear();

        if (total == 0) {
            historico.setSituacaoFinal("CURSANDO");
        } else if (anoLetivo >= anoAtual) {
            historico.setSituacaoFinal("CURSANDO");
        } else if (aprovadas * 2 >= total) {
            historico.setSituacaoFinal("APROVADO");
        } else {
            historico.setSituacaoFinal("REPROVADO");
        }

        // ===== 7. Carga horária total =====
        int cargaTotal = historico.getItems().stream()
                .mapToInt(i -> i.getCargaHoraria() != null ? i.getCargaHoraria() : 0)
                .sum();
        historico.setCargaHorariaTotal(cargaTotal);

        // ===== 8. Dias letivos (padrão 200) =====
        historico.setDiasLetivos(200);

        // ===== 9. Salvar =====
        return historicoRepository.save(historico);
    }

    // ==================================================================
    // SALVAR (UPDATE) — recalcula resultado dos itens e situação final
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

                // Recalcula resultado do item (exceto se estiver CURSANDO)
                double nota = item.getNotaFinal() != null ? item.getNotaFinal() : 0.0;
                if (!"CURSANDO".equalsIgnoreCase(item.getResultado())) {
                    item.setResultado(nota >= media ? "APROVADO" : "REPROVADO");
                }
            }
        }

        // Recalcula situação final se não estiver definida manualmente
        if (historico.getSituacaoFinal() == null || historico.getSituacaoFinal().isEmpty()) {
            int total = historico.getItems().size();
            int aprovadas = historico.getTotalAprovadas();
            int anoAtual = LocalDate.now().getYear();

            if (total == 0 || (historico.getAnoLetivo() != null && historico.getAnoLetivo() >= anoAtual)) {
                historico.setSituacaoFinal("CURSANDO");
            } else if (aprovadas * 2 >= total) {
                historico.setSituacaoFinal("APROVADO");
            } else {
                historico.setSituacaoFinal("REPROVADO");
            }
        }

        // Recalcula carga horária
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
    // RECALCULAR (busca notas DO ANO do histórico + faltas atualizadas)
    // ==================================================================
    @Transactional
    public HistoricoEscolar recalcular(Long id) {
        HistoricoEscolar historico = buscarPorId(id);
        if (historico == null) {
            throw new RuntimeException("Histórico não encontrado: " + id);
        }

        Long alunoId = historico.getAluno().getId();
        Integer anoLetivo = historico.getAnoLetivo();
        double media = historico.getMediaAprovacaoSegura();

        // ⚠️ Notas filtradas pelo ano do histórico
        List<Nota> notas = notaService.listarPorAlunoEAno(alunoId, anoLetivo);
        List<Frequencia> frequencias = frequenciaService.listarPorAluno(alunoId);

        // Recalcular faltas
        Map<Long, Integer> faltasPorMateria = new HashMap<>();
        for (Frequencia f : frequencias) {
            if (Boolean.FALSE.equals(f.getPresente()) && f.getMateria() != null) {
                faltasPorMateria.merge(f.getMateria().getId(), 1, Integer::sum);
            }
        }

        // Recalcular nota final de cada item
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

        // Situação final
        int total = historico.getItems().size();
        int aprovadas = historico.getTotalAprovadas();
        int anoAtual = LocalDate.now().getYear();

        if (total == 0 || (anoLetivo != null && anoLetivo >= anoAtual)) {
            historico.setSituacaoFinal("CURSANDO");
        } else if (aprovadas * 2 >= total) {
            historico.setSituacaoFinal("APROVADO");
        } else {
            historico.setSituacaoFinal("REPROVADO");
        }

        // Carga horária
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
}