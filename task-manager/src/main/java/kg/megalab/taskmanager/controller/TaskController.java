package kg.megalab.taskmanager.controller;

import jakarta.validation.Valid;
import kg.megalab.taskmanager.dto.task.CreateTaskRequest;
import kg.megalab.taskmanager.dto.task.TaskDetailResponse;
import kg.megalab.taskmanager.dto.task.TaskListItemResponse;
import kg.megalab.taskmanager.dto.task.UpdateTaskRequest;
import kg.megalab.taskmanager.service.TaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskListItemResponse> list(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) UUID departmentId,
                                            @RequestParam(required = false) UUID creatorId,
                                            @RequestParam(required = false) String priority,
                                            @RequestParam(required = false) String q) {
        return taskService.list(status, departmentId, creatorId, priority, q);
    }

    @GetMapping("/{id}")
    public TaskDetailResponse get(@PathVariable UUID id) {
        return taskService.getDetail(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public TaskDetailResponse create(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public TaskDetailResponse update(@PathVariable UUID id, @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        taskService.delete(id);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD')")
    public TaskDetailResponse close(@PathVariable UUID id) {
        return taskService.close(id);
    }
}
