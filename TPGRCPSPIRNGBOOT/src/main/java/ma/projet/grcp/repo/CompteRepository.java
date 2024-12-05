package ma.projet.grcp.repo;

import ma.projet.grcp.entity.Compte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompteRepository extends JpaRepository<Compte, String> {

}
