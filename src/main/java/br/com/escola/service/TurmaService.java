package br.com.escola.service;

import br.com.escola.model.Turma;
import br.com.escola.repository.TurmaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TurmaService {

    @Autowired
    private TurmaRepository turmaRepository;

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
}