package br.com.escola.repository;

import br.com.escola.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotaRepository extends JpaRepository<Nota, Long> {
    List<Nota> findByAlunoId(Long alunoId);
    List<Nota> findByAlunoIdAndMateriaId(Long alunoId, Long materiaId);
    List<Nota> findByAlunoIdAndMateriaIdAndUnidade(Long alunoId, Long materiaId, Integer unidade);
}