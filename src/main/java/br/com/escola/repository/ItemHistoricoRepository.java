package br.com.escola.repository;

import br.com.escola.model.ItemHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemHistoricoRepository extends JpaRepository<ItemHistorico, Long> {

    /**
     * Lista os itens de um histórico, ordenados por ordem de exibição.
     */
    List<ItemHistorico> findByHistoricoIdOrderByOrdemAsc(Long historicoId);

    /**
     * Exclui todos os itens de um histórico.
     */
    void deleteByHistoricoId(Long historicoId);

    /**
     * Conta os itens de um histórico.
     */
    long countByHistoricoId(Long historicoId);
}
