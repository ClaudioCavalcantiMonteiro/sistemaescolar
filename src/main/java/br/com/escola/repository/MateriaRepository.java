package br.com.escola.repository;

import br.com.escola.model.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Long> {
    Optional<Materia> findByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCase(String nome);
}
