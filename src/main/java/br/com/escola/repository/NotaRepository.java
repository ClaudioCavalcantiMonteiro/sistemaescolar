package br.com.escola.repository;

import br.com.escola.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotaRepository extends JpaRepository<Nota, Long> {

    // ===== Métodos originais =====
    List<Nota> findByAlunoId(Long alunoId);

    List<Nota> findByAlunoIdAndMateriaId(Long alunoId, Long materiaId);

    List<Nota> findByAlunoIdAndMateriaIdAndUnidade(Long alunoId, Long materiaId, Integer unidade);

    List<Nota> findByMateriaId(Long materiaId);

    // ===== NOVOS métodos filtrando por ano letivo =====

    /**
     * Lista todas as notas de um aluno em um ano letivo específico.
     */
    List<Nota> findByAlunoIdAndAnoLetivo(Long alunoId, Integer anoLetivo);

    /**
     * Lista notas do aluno em uma matéria, em um ano letivo específico.
     */
    List<Nota> findByAlunoIdAndMateriaIdAndAnoLetivo(Long alunoId, Long materiaId, Integer anoLetivo);

    /**
     * Busca nota específica de aluno + matéria + unidade + ano letivo.
     * Usado ao lançar/atualizar notas para não misturar anos.
     */
    List<Nota> findByAlunoIdAndMateriaIdAndUnidadeAndAnoLetivo(
            Long alunoId, Long materiaId, Integer unidade, Integer anoLetivo);
}