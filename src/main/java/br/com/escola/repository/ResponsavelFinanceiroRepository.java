package br.com.escola.repository;

import br.com.escola.model.ResponsavelFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResponsavelFinanceiroRepository extends JpaRepository<ResponsavelFinanceiro, Long> {
    Optional<ResponsavelFinanceiro> findByAlunoId(Long alunoId);
    Optional<ResponsavelFinanceiro> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
}