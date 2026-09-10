package br.com.escola.repository;

import br.com.escola.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TurmaRepository extends JpaRepository<Turma, Long> {

    List<Turma> findBySerieId(Long serieId);

    @Query("SELECT t FROM Turma t LEFT JOIN FETCH t.alunos")
    List<Turma> findAllWithAlunos();

    @Query("SELECT t FROM Turma t LEFT JOIN FETCH t.alunos WHERE t.id = :id")
    Turma findByIdWithAlunos(Long id);
}