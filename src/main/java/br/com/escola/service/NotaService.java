package br.com.escola.service;

import br.com.escola.model.Nota;
import br.com.escola.repository.NotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotaService {

    @Autowired
    private NotaRepository notaRepository;

    public Nota salvar(Nota nota) {
        return notaRepository.save(nota);
    }

    public List<Nota> listarPorAluno(Long alunoId) {
        return notaRepository.findByAlunoId(alunoId);
    }

    public List<Nota> listarPorAlunoEMateria(Long alunoId, Long materiaId) {
        return notaRepository.findByAlunoIdAndMateriaId(alunoId, materiaId);
    }

    public List<Nota> buscarPorAlunoMateriaEUnidade(Long alunoId, Long materiaId, Integer unidade) {
        return notaRepository.findByAlunoIdAndMateriaIdAndUnidade(alunoId, materiaId, unidade);
    }

    public void excluir(Long id) {
        notaRepository.deleteById(id);
    }

    public void excluirPorAlunoId(Long alunoId) {
        List<Nota> notas = notaRepository.findByAlunoId(alunoId);
        if (!notas.isEmpty()) {
            notaRepository.deleteAll(notas);
        }
    }

    // ==================================================================
    // METODOS NOVOS - USADOS PELO DASHBOARD
    // ==================================================================

    /**
     * Calcula a media geral da escola (media de todas as medias finais).
     */
    public Double calcularMediaGeralEscola() {
        List<Nota> todas = notaRepository.findAll();
        if (todas.isEmpty()) return 0.0;

        double soma = 0;
        int count = 0;
        for (Nota n : todas) {
            if (n.getMediaFinal() != null) {
                soma += n.getMediaFinal();
                count++;
            }
        }
        return count > 0 ? soma / count : 0.0;
    }

    /**
     * Calcula a media de uma materia especifica.
     */
    public Double calcularMediaPorMateria(Long materiaId) {
        List<Nota> notas = notaRepository.findByMateriaId(materiaId);
        if (notas.isEmpty()) return 0.0;

        double soma = 0;
        int count = 0;
        for (Nota n : notas) {
            if (n.getMediaFinal() != null) {
                soma += n.getMediaFinal();
                count++;
            }
        }
        return count > 0 ? soma / count : 0.0;
    }

    /**
     * Conta quantos alunos estao aprovados e reprovados.
     * Retorna [aprovados, reprovados].
     *
     * Regra: media geral do aluno >= 6 = aprovado, senao reprovado.
     */
    public int[] contarAprovadosReprovados() {
        List<Nota> todas = notaRepository.findAll();

        // Agrupa notas por aluno
        java.util.Map<Long, List<Double>> mediasPorAluno = new java.util.HashMap<>();

        for (Nota n : todas) {
            if (n.getAluno() == null || n.getMediaFinal() == null) continue;
            Long alunoId = n.getAluno().getId();
            mediasPorAluno.computeIfAbsent(alunoId, k -> new ArrayList<>())
                    .add(n.getMediaFinal());
        }

        int aprovados = 0;
        int reprovados = 0;

        for (List<Double> medias : mediasPorAluno.values()) {
            double soma = 0;
            for (Double m : medias) soma += m;
            double mediaAluno = soma / medias.size();

            if (mediaAluno >= 6.0) {
                aprovados++;
            } else {
                reprovados++;
            }
        }

        return new int[]{aprovados, reprovados};
    }
}