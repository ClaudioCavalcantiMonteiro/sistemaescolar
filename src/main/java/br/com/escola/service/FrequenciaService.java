package br.com.escola.service;

import br.com.escola.model.Frequencia;
import br.com.escola.repository.FrequenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class FrequenciaService {

    @Autowired
    private FrequenciaRepository frequenciaRepository;

    public Frequencia salvar(Frequencia frequencia) {
        return frequenciaRepository.save(frequencia);
    }

    public List<Frequencia> listarTodas() {
        return frequenciaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Frequencia> listarPorAluno(Long alunoId) {
        return frequenciaRepository.findByAlunoIdWithDetails(alunoId);
    }

    @Transactional(readOnly = true)
    public List<Frequencia> listarPorTurma(Long turmaId) {
        return frequenciaRepository.findByTurmaId(turmaId);
    }

    @Transactional(readOnly = true)
    public List<Frequencia> listarPorTurmaEData(Long turmaId, LocalDate data) {
        return frequenciaRepository.findByTurmaAndDataWithDetails(turmaId, data);
    }

    @Transactional(readOnly = true)
    public List<Frequencia> listarPorTurmaEPeriodo(Long turmaId, LocalDate inicio, LocalDate fim) {
        return frequenciaRepository.findByTurmaAndPeriodo(turmaId, inicio, fim);
    }

    public Frequencia buscarPorAlunoTurmaData(Long alunoId, Long turmaId, LocalDate data) {
        List<Frequencia> lista = frequenciaRepository.findByAlunoIdAndData(alunoId, data);
        for (Frequencia f : lista) {
            if (f.getTurma() != null && f.getTurma().getId().equals(turmaId)) {
                return f;
            }
        }
        return null;
    }

    // Busca por aluno + turma + matéria + data
    public Frequencia buscarPorAlunoTurmaMateriaData(Long alunoId, Long turmaId, Long materiaId, LocalDate data) {
        List<Frequencia> lista = frequenciaRepository.findByAlunoIdAndData(alunoId, data);
        for (Frequencia f : lista) {
            boolean mesmaTurma = f.getTurma() != null && f.getTurma().getId().equals(turmaId);
            boolean mesmaMateria = (materiaId == null && f.getMateria() == null) ||
                    (materiaId != null && f.getMateria() != null && f.getMateria().getId().equals(materiaId));
            if (mesmaTurma && mesmaMateria) {
                return f;
            }
        }
        return null;
    }

    public void excluir(Long id) {
        frequenciaRepository.deleteById(id);
    }

    public EstatisticasFrequencia calcularEstatisticas(Long alunoId) {
        List<Frequencia> registros = frequenciaRepository.findByAlunoId(alunoId);
        return calcular(registros);
    }

    public EstatisticasFrequencia calcularEstatisticasPeriodo(Long alunoId, LocalDate inicio, LocalDate fim) {
        List<Frequencia> registros = frequenciaRepository.findByAlunoAndPeriodo(alunoId, inicio, fim);
        return calcular(registros);
    }

    private EstatisticasFrequencia calcular(List<Frequencia> registros) {
        int total = registros.size();
        int presencas = 0;
        int faltas = 0;

        for (Frequencia f : registros) {
            if (Boolean.TRUE.equals(f.getPresente())) {
                presencas++;
            } else {
                faltas++;
            }
        }

        double percentual = total > 0 ? (presencas * 100.0 / total) : 0.0;

        return new EstatisticasFrequencia(total, presencas, faltas,
                Math.round(percentual * 10.0) / 10.0);
    }

    // ========== Classe interna ==========
    public static class EstatisticasFrequencia {
        private final int total;
        private final int presencas;
        private final int faltas;
        private final double percentual;

        public EstatisticasFrequencia(int total, int presencas, int faltas, double percentual) {
            this.total = total;
            this.presencas = presencas;
            this.faltas = faltas;
            this.percentual = percentual;
        }

        public int getTotal() { return total; }
        public int getPresencas() { return presencas; }
        public int getFaltas() { return faltas; }
        public double getPercentual() { return percentual; }

        public boolean isAbaixoMinimo() { return percentual < 75.0 && total > 0; }
    }
}