package nl.inholland.bankAppBackEnd.repository;

import nl.inholland.bankAppBackEnd.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByNameContainingIgnoreCase(String name);
    Optional<User> findByBsnNumber(String bsnNumber);

}
