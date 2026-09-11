package travel.winwin.authapi.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import travel.winwin.authapi.entity.User;

/** Spring Data generates the implementation, including SQL derived from the method names. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
