package kg.megalab.taskmanager.config;

import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.User;
import kg.megalab.taskmanager.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a single administrator account so there's a way to log in and provision everything
 * else (departments, users, tasks) through the admin panel. Password: "Passw0rd!".
 */
@Component
@Transactional
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_PASSWORD = "Passw0rd!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = new User();
        admin.setFullName("Администратор Системы");
        admin.setLogin("admin");
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }
}
