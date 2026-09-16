package br.com.escola.repository;

import br.com.escola.model.Licenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LicencaRepository extends JpaRepository<Licenca, Long> {

    /**
     * Como só existe UMA licença por instalação, buscamos a primeira.
     */
    default Optional<Licenca> buscarUnica() {
        return findAll().stream().findFirst();
    }
}
