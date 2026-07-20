package kg.megalab.taskmanager.controller;

import jakarta.validation.Valid;
import kg.megalab.taskmanager.dto.subtask.CreateSubtaskRequest;
import kg.megalab.taskmanager.dto.subtask.SubtaskResponse;
import kg.megalab.taskmanager.dto.subtask.ToggleSubtaskRequest;
import kg.megalab.taskmanager.dto.subtask.UpdateSubtaskRequest;
import kg.megalab.taskmanager.service.SubtaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SubtaskController {

    private final SubtaskService subtaskService;

    public SubtaskController(SubtaskService subtaskService) {
        this.subtaskService = subtaskService;
    }

    @PostMapping("/tasks/{taskId}/subtasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD', 'HEAD')")
    public SubtaskResponse create(@PathVariable UUID taskId, @Valid @RequestBody CreateSubtaskRequest request) {
        return subtaskService.create(taskId, request);
    }

    @PatchMapping("/subtasks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD')")
    public SubtaskResponse update(@PathVariable UUID id, @RequestBody UpdateSubtaskRequest request) {
        return subtaskService.update(id, request);
    }

    @PatchMapping("/subtasks/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD', 'EMPLOYEE')")
    public SubtaskResponse toggle(@PathVariable UUID id,
                                   @RequestBody(required = false) ToggleSubtaskRequest request) {
        return subtaskService.toggle(id, request);
    }

    @DeleteMapping("/subtasks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public void delete(@PathVariable UUID id) {
        subtaskService.delete(id);
    }
}
