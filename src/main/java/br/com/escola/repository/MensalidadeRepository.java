package br.com.escola.repository;

import br.com.escola.model.Mensalidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MensalidadeRepository extends JpaRepository<Mensalidade, Long> {

    List<Mensalidade> findByAlunoId(Long alunoId);

    List<Mensalidade> findByStatus(String status);

    List<Mensalidade> findByMesReferenciaBetween(LocalDate inicio, LocalDate fim);

    @Query("SELECT m FROM Mensalidade m " +
           "LEFT JOIN FETCH m.aluno a " +
           "LEFT JOIN FETCH a.turma " +
           "WHERE m.aluno.id = :alunoId " +
           "ORDER BY m.mesReferencia DESC")
    List<Mensalidade> findByAlunoWithDetails(@Param("alunoId") Long alunoId);

    @Query("SELECT m FROM Mensalidade m " +
           "LEFT JOIN FETCH m.aluno a " +
           "LEFT JOIN FETCH a.turma t " +
           "WHERE m.mesReferencia BETWEEN :inicio AND :fim " +
           "ORDER BY m.dataVencimento, a.nome")
    List<Mensalidade> findByPeriodoWithDetails(@Param("inicio") LocalDate inicio,
                                                @Param("fim") LocalDate fim);

    @Query("SELECT m FROM Mensalidade m " +
           "LEFT JOIN FETCH m.aluno a " +
           "WHERE m.mesReferencia BETWEEN :inicio AND :fim " +
           "AND a.turma.id = :turmaId " +
           "ORDER BY a.nome")
    List<Mensalidade> findByTurmaAndPeriodo(@Param("turmaId") Long turmaId,
                                             @Param("inicio") LocalDate inicio,
                                             @Param("fim") LocalDate fim);

    @Query("SELECT m FROM Mensalidade m " +
           "WHERE m.aluno.id = :alunoId " +
           "AND m.mesReferencia = :mesReferencia")
    Mensalidade findByAlunoAndMes(@Param("alunoId") Long alunoId,
                                   @Param("mesReferencia") LocalDate mesReferencia);

    @Query("SELECT m FROM Mensalidade m " +
           "LEFT JOIN FETCH m.aluno a " +
           "WHERE m.status = 'PENDENTE' " +
           "AND m.dataVencimento < :hoje " +
           "ORDER BY m.dataVencimento")
    List<Mensalidade> findAtrasadas(@Param("hoje") LocalDate hoje);
}
