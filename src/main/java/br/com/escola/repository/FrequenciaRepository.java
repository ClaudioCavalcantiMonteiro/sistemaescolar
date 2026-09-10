package br.com.escola.repository;

import br.com.escola.model.Frequencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FrequenciaRepository extends JpaRepository<Frequencia, Long> {

    List<Frequencia> findByAlunoId(Long alunoId);

    List<Frequencia> findByTurmaId(Long turmaId);

    List<Frequencia> findByAlunoIdAndData(Long alunoId, LocalDate data);

    List<Frequencia> findByTurmaIdAndData(Long turmaId, LocalDate data);

    @Query("SELECT f FROM Frequencia f " +
           "LEFT JOIN FETCH f.aluno " +
           "LEFT JOIN FETCH f.turma " +
           "LEFT JOIN FETCH f.materia " +
           "WHERE f.turma.id = :turmaId AND f.data = :data " +
           "ORDER BY f.materia.id, f.aluno.nome")
    List<Frequencia> findByTurmaAndDataWithDetails(@Param("turmaId") Long turmaId,
                                                    @Param("data") LocalDate data);

    @Query("SELECT f FROM Frequencia f " +
           "LEFT JOIN FETCH f.aluno " +
           "LEFT JOIN FETCH f.turma " +
           "LEFT JOIN FETCH f.materia " +
           "WHERE f.aluno.id = :alunoId " +
           "ORDER BY f.data DESC")
    List<Frequencia> findByAlunoIdWithDetails(@Param("alunoId") Long alunoId);

    @Query("SELECT f FROM Frequencia f " +
           "LEFT JOIN FETCH f.aluno " +
           "LEFT JOIN FETCH f.turma " +
           "LEFT JOIN FETCH f.materia " +
           "WHERE f.turma.id = :turmaId " +
           "AND f.data BETWEEN :inicio AND :fim")
    List<Frequencia> findByTurmaAndPeriodo(@Param("turmaId") Long turmaId,
                                            @Param("inicio") LocalDate inicio,
                                            @Param("fim") LocalDate fim);

    @Query("SELECT f FROM Frequencia f " +
           "WHERE f.aluno.id = :alunoId " +
           "AND f.data BETWEEN :inicio AND :fim")
    List<Frequencia> findByAlunoAndPeriodo(@Param("alunoId") Long alunoId,
                                            @Param("inicio") LocalDate inicio,
                                            @Param("fim") LocalDate fim);
}