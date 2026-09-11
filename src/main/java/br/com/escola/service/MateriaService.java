package br.com.escola.service;

import br.com.escola.model.Materia;
import br.com.escola.repository.MateriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MateriaService {

    @Autowired
    private MateriaRepository materiaRepository;

    public List<Materia> listarTodas() {
        return materiaRepository.findAll();
    }

    public Materia salvar(Materia materia) {
        return materiaRepository.save(materia);
    }

    public Materia buscarPorId(Long id) {
        return materiaRepository.findById(id).orElse(null);
    }

    public void excluir(Long id) {
        materiaRepository.deleteById(id);
    }

    public boolean existeNome(String nome) {
        return materiaRepository.existsByNomeIgnoreCase(nome);
    }
}