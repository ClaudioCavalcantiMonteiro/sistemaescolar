package br.com.escola.service;

import br.com.escola.model.Escola;
import br.com.escola.repository.EscolaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EscolaService {

    @Autowired
    private EscolaRepository escolaRepository;

    /**
     * Retorna a escola cadastrada. Como só existe UMA escola no sistema,
     * retorna o primeiro registro (ou null se não existir).
     */
    public Escola buscarEscola() {
        List<Escola> lista = escolaRepository.findAll();
        if (lista.isEmpty()) {
            return null;
        }
        return lista.get(0);
    }

    public Escola salvar(Escola escola) {
        return escolaRepository.save(escola);
    }

    public Escola buscarPorId(Long id) {
        return escolaRepository.findById(id).orElse(null);
    }
}
