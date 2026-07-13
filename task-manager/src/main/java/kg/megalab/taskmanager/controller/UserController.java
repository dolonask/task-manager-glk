package kg.megalab.taskmanager.controller;

import jakarta.validation.Valid;
import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.dto.user.CreateUserRequest;
import kg.megalab.taskmanager.dto.user.UpdateUserRequest;
import kg.megalab.taskmanager.dto.user.UserResponse;
import kg.megalab.taskmanager.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) String role,
                                    @RequestParam(required = false) UUID departmentId,
                                    @RequestParam(required = false) Boolean isActive) {
        return userService.list(role != null ? Role.fromValue(role) : null, departmentId, isActive);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return userService.get(id);
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }
}
