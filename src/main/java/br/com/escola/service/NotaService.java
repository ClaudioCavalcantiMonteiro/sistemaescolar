package br.com.escola.service;

import br.com.escola.model.Nota;
import br.com.escola.repository.NotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
}