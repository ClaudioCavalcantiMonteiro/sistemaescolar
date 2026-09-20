package br.com.escola.repository;

import br.com.escola.model.HistoricoEscolar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricoEscolarRepository extends JpaRepository<HistoricoEscolar, Long> {

    /**
     * Busca todos os históricos de um aluno, ordenados por ano letivo (mais recente primeiro).
     */
    List<HistoricoEscolar> findByAlunoIdOrderByAnoLetivoDesc(Long alunoId);

    /**
     * Busca o histórico específico de um aluno em um determinado ano letivo.
     */
    Optional<HistoricoEscolar> findByAlunoIdAndAnoLetivo(Long alunoId, Integer anoLetivo);

    /**
     * Verifica se já existe histórico para o aluno no ano letivo.
     */
    boolean existsByAlunoIdAndAnoLetivo(Long alunoId, Integer anoLetivo);

    /**
     * Busca um histórico pelo ID carregando os items (matérias) de uma vez.
     * Evita LazyInitializationException.
     */
    @Query("SELECT DISTINCT h FROM HistoricoEscolar h " +
           "LEFT JOIN FETCH h.items i " +
           "LEFT JOIN FETCH i.materia " +
           "WHERE h.id = :id")
    Optional<HistoricoEscolar> findByIdWithItems(@Param("id") Long id);

    /**
     * Busca todos os históricos carregando os items (para listagem completa).
     */
    @Query("SELECT DISTINCT h FROM HistoricoEscolar h " +
           "LEFT JOIN FETCH h.items " +
           "ORDER BY h.anoLetivo DESC, h.id DESC")
    List<HistoricoEscolar> findAllWithItems();

    /**
     * Busca históricos de um aluno carregando os items.
     */
    @Query("SELECT DISTINCT h FROM HistoricoEscolar h " +
           "LEFT JOIN FETCH h.items " +
           "WHERE h.aluno.id = :alunoId " +
           "ORDER BY h.anoLetivo DESC")
    List<HistoricoEscolar> findByAlunoIdWithItems(@Param("alunoId") Long alunoId);

    /**
     * Conta quantos históricos existem para um aluno.
     */
    long countByAlunoId(Long alunoId);
}
