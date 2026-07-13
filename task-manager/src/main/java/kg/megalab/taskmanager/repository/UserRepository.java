package kg.megalab.taskmanager.repository;

import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    long countByRole(Role role);
}
