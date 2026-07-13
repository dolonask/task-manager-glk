package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.User;
import kg.megalab.taskmanager.dto.user.CreateUserRequest;
import kg.megalab.taskmanager.dto.user.UpdateUserRequest;
import kg.megalab.taskmanager.dto.user.UserResponse;
import kg.megalab.taskmanager.exception.ConflictException;
import kg.megalab.taskmanager.exception.NotFoundException;
import kg.megalab.taskmanager.repository.DepartmentRepository;
import kg.megalab.taskmanager.repository.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, DepartmentRepository departmentRepository,
                        PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(Role role, UUID departmentId, Boolean isActive) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }
        return userRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByLogin(request.login())) {
            throw new ConflictException("Пользователь с логином \"" + request.login() + "\" уже существует");
        }
        User user = new User();
        user.setFullName(request.fullName());
        user.setLogin(request.login());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.fromValue(request.role()));
        if (request.departmentId() != null) {
            user.setDepartment(departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new NotFoundException("Department", request.departmentId())));
        }
        user = userRepository.save(user);
        auditLogService.record("User", user.getId(), "user.create", null, toResponse(user));
        return toResponse(user);
    }

    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findOrThrow(id);
        UserResponse before = toResponse(user);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.login() != null && !request.login().equals(user.getLogin())) {
            if (userRepository.existsByLogin(request.login())) {
                throw new ConflictException("Пользователь с логином \"" + request.login() + "\" уже существует");
            }
            user.setLogin(request.login());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(Role.fromValue(request.role()));
        }
        if (request.departmentId() != null) {
            user.setDepartment(departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new NotFoundException("Department", request.departmentId())));
        }
        if (request.isActive() != null) {
            user.setActive(request.isActive());
        }
        user = userRepository.save(user);
        auditLogService.record("User", user.getId(), "user.update", before, toResponse(user));
        return toResponse(user);
    }

    User findOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    UserResponse toResponse(User user) {
        List<UUID> curated = departmentRepository.findByCuratorId(user.getId()).stream()
                .map(kg.megalab.taskmanager.domain.Department::getId).toList();
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getLogin(),
                user.getRole().getValue(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                curated,
                user.isActive()
        );
    }
}
