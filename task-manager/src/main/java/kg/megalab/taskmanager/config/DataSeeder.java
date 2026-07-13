package kg.megalab.taskmanager.config;

import kg.megalab.taskmanager.domain.Department;
import kg.megalab.taskmanager.domain.Priority;
import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.Task;
import kg.megalab.taskmanager.domain.User;
import kg.megalab.taskmanager.repository.DepartmentRepository;
import kg.megalab.taskmanager.repository.TaskRepository;
import kg.megalab.taskmanager.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Seeds demo accounts/departments/tasks so the frontend's "Демо-пользователь" login select
 * (README §1 "Вход") and RBAC flows are exercisable without a real AD/LDAP connection.
 * Demo password for every account below is "Passw0rd!".
 */
@Component
@Transactional
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Passw0rd!";

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, DepartmentRepository departmentRepository,
                       TaskRepository taskRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        createUser("Администратор Системы", "admin", Role.ADMIN, null);

        User board1 = createUser("Осмонов Нурлан Асанович", "osmonov.n", Role.BOARD, null);
        User board2 = createUser("Жумабекова Айгуль Токтогуловна", "zhumabekova.a", Role.BOARD, null);
        User board3 = createUser("Садыков Марат Бекболотович", "sadykov.m", Role.BOARD, null);

        Department leasing = createDepartment("Департамент лизинговых операций", board1, null);
        Department legal = createDepartment("Юридический департамент", board2, null);
        Department finance = createDepartment("Финансовый департамент", board3, null);

        User head1 = createUser("Абдылдаев Улан Раимович", "abdyldaev.u", Role.HEAD, leasing);
        User head2 = createUser("Мамбетова Чолпон Асылбековна", "mambetova.ch", Role.HEAD, legal);
        User head3 = createUser("Тестов текст Бакыт Эсенович", "toktosunov.b", Role.HEAD, finance);

        leasing.setHead(head1);
        legal.setHead(head2);
        finance.setHead(head3);
        departmentRepository.save(leasing);
        departmentRepository.save(legal);
        departmentRepository.save(finance);

        createUser("Асанов Данияр Муратович", "asanov.d", Role.EMPLOYEE, leasing);
        createUser("Наблюдатель Демо", "observer", Role.OBSERVER, null);

        createTask("Подготовить квартальный отчёт по лизинговому портфелю", leasing, board1, head1,
                Priority.HIGH, LocalDate.now().plusDays(10));
        createTask("Актуализировать типовой договор лизинга", legal, board2, head2,
                Priority.MEDIUM, LocalDate.now().plusDays(20));
        createTask("Провести сверку просроченной задолженности", finance, board3, head3,
                Priority.HIGH, LocalDate.now().minusDays(2));
    }

    private User createUser(String fullName, String login, Role role, Department department) {
        User user = new User();
        user.setFullName(fullName);
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setDepartment(department);
        return userRepository.save(user);
    }

    private Department createDepartment(String name, User curator, User head) {
        Department department = new Department();
        department.setName(name);
        department.setCurator(curator);
        department.setHead(head);
        return departmentRepository.save(department);
    }

    private void createTask(String title, Department department, User creator, User assignee,
                             Priority priority, LocalDate deadline) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(title);
        task.setDepartment(department);
        task.setCreator(creator);
        task.setAssignee(assignee);
        task.setPriority(priority);
        task.setInitialDeadline(deadline);
        task.setCurrentDeadline(deadline);
        taskRepository.save(task);
    }
}
