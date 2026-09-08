package br.com.escola.repository;

import br.com.escola.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TurmaRepository extends JpaRepository<Turma, Long> {
    List<Turma> findBySerieId(Long serieId);
}