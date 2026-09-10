package br.com.escola.service;

import br.com.escola.model.ResponsavelFinanceiro;
import br.com.escola.repository.ResponsavelFinanceiroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResponsavelFinanceiroService {

    @Autowired
    private ResponsavelFinanceiroRepository responsavelRepository;

    public ResponsavelFinanceiro salvar(ResponsavelFinanceiro responsavel) {
        return responsavelRepository.save(responsavel);
    }

    public ResponsavelFinanceiro buscarPorAlunoId(Long alunoId) {
        return responsavelRepository.findByAlunoId(alunoId).orElse(null);
    }

    public void excluir(Long id) {
        responsavelRepository.deleteById(id);
    }

    public void excluirPorAlunoId(Long alunoId) {
        responsavelRepository.findByAlunoId(alunoId).ifPresent(responsavelRepository::delete);
    }
}