package br.com.escola.service;

import br.com.escola.model.Aluno;
import br.com.escola.repository.AlunoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AlunoService {

    @Autowired
    private AlunoRepository alunoRepository;

    public List<Aluno> listarTodos() {
        return alunoRepository.findAll();
    }

    public Aluno salvar(Aluno aluno) {
        return alunoRepository.save(aluno);
    }

    public Aluno buscarPorId(Long id) {
        return alunoRepository.findById(id).orElse(null);
    }

    public void excluir(Long id) {
        alunoRepository.deleteById(id);
    }

    public List<Aluno> buscarPorSerie(Long serieId) {
        return alunoRepository.findBySerieId(serieId);
    }

    public List<Aluno> buscarPorTurma(Long turmaId) {
        return alunoRepository.findByTurmaId(turmaId);
    }
}