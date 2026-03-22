package backend.repositories;

import backend.models.WatchParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WatchPartyRepository extends JpaRepository<WatchParty, Long> {
    // Find a party by its name since name is not the ID anymore
    Optional<WatchParty> findByName(String name);
}