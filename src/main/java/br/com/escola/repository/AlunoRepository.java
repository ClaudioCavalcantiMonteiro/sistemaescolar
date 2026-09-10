package br.com.escola.repository;

import br.com.escola.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    List<Aluno> findBySerieId(Long serieId);

    List<Aluno> findByTurmaId(Long turmaId);

    // NOVO: Busca alunos com todos os relacionamentos carregados
    @Query("SELECT DISTINCT a FROM Aluno a " +
           "LEFT JOIN FETCH a.serie " +
           "LEFT JOIN FETCH a.turma " +
           "LEFT JOIN FETCH a.responsavelFinanceiro " +
           "WHERE a.turma.id = :turmaId")
    List<Aluno> findByTurmaIdWithDetails(@Param("turmaId") Long turmaId);
}