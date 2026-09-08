package br.com.escola.service;

import br.com.escola.model.Serie;
import br.com.escola.repository.SerieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("unused")
@Service
public class SerieService {

    private final SerieRepository serieRepository;

    SerieService(SerieRepository serieRepository) {
        this.serieRepository = serieRepository;
    }

    public List<Serie> listarTodas() {
        return serieRepository.findAll();
    }

    public Serie salvar(Serie serie) {
        return serieRepository.save(serie);
    }

    public Serie buscarPorId(Long id) {
        return serieRepository.findById(id).orElse(null);
    }

    public void excluir(Long id) {
        serieRepository.deleteById(id);
    }
}