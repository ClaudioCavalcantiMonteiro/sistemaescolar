package br.com.escola.repository;

import br.com.escola.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    List<Aluno> findBySerieId(Long serieId);
    List<Aluno> findByTurmaId(Long turmaId);
}