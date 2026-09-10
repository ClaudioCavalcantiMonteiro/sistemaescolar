package br.com.escola.service;

import br.com.escola.model.Aluno;
import br.com.escola.model.Turma;
import br.com.escola.repository.AlunoRepository;
import br.com.escola.repository.TurmaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TurmaService {

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private AlunoRepository alunoRepository;

    public List<Turma> listarTodas() {
        return turmaRepository.findAll();
    }

    public Turma salvar(Turma turma) {
        return turmaRepository.save(turma);
    }

    public Turma buscarPorId(Long id) {
        return turmaRepository.findById(id).orElse(null);
    }

    public void excluir(Long id) {
        turmaRepository.deleteById(id);
    }

    public List<Turma> buscarPorSerie(Long serieId) {
        return turmaRepository.findBySerieId(serieId);
    }

    @Transactional(readOnly = true)
    public List<Turma> listarTodasComAlunos() {
        return turmaRepository.findAllWithAlunos();
    }

    @Transactional(readOnly = true)
    public List<Aluno> buscarAlunosPorTurma(Long turmaId) {
        List<Aluno> alunos = alunoRepository.findByTurmaIdWithDetails(turmaId);
        return alunos != null ? alunos : new ArrayList<>();
    }
}