package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.Subtask;
import kg.megalab.taskmanager.domain.Task;
import kg.megalab.taskmanager.dto.subtask.CreateSubtaskRequest;
import kg.megalab.taskmanager.dto.subtask.SubtaskResponse;
import kg.megalab.taskmanager.dto.subtask.UpdateSubtaskRequest;
import kg.megalab.taskmanager.exception.ForbiddenException;
import kg.megalab.taskmanager.exception.NotFoundException;
import kg.megalab.taskmanager.repository.SubtaskRepository;
import kg.megalab.taskmanager.repository.UserRepository;
import kg.megalab.taskmanager.security.SecurityUtils;
import kg.megalab.taskmanager.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final AuditLogService auditLogService;

    public SubtaskService(SubtaskRepository subtaskRepository, UserRepository userRepository,
                           TaskService taskService, AuditLogService auditLogService) {
        this.subtaskRepository = subtaskRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.auditLogService = auditLogService;
    }

    private void assertDecomposeAccess(Task task) {
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean allowed = principal.role() == Role.ADMIN || principal.role() == Role.BOARD
                || (principal.role() == Role.HEAD && principal.belongsTo(task.getDepartment().getId()));
        if (!allowed) {
            throw new ForbiddenException("Нет прав на декомпозицию этой задачи");
        }
    }

    public SubtaskResponse create(UUID taskId, CreateSubtaskRequest request) {
        Task task = taskService.findOrThrow(taskId);
        assertDecomposeAccess(task);

        Subtask subtask = new Subtask();
        subtask.setTask(task);
        subtask.setTitle(request.title());
        subtask.setDeadline(request.deadline());
        if (request.assigneeId() != null) {
            subtask.setAssignee(userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new NotFoundException("User", request.assigneeId())));
        }
        subtask = subtaskRepository.save(subtask);
        auditLogService.record("Subtask", subtask.getId(), "subtask.create", null, toResponse(subtask));
        return toResponse(subtask);
    }

    public SubtaskResponse update(UUID id, UpdateSubtaskRequest request) {
        Subtask subtask = findOrThrow(id);
        assertDecomposeAccess(subtask.getTask());

        SubtaskResponse before = toResponse(subtask);
        if (request.title() != null) {
            subtask.setTitle(request.title());
        }
        if (request.deadline() != null) {
            subtask.setDeadline(request.deadline());
        }
        if (request.assigneeId() != null) {
            subtask.setAssignee(userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new NotFoundException("User", request.assigneeId())));
        }
        subtask = subtaskRepository.save(subtask);
        auditLogService.record("Subtask", subtask.getId(), "subtask.update", before, toResponse(subtask));
        return toResponse(subtask);
    }

    public SubtaskResponse toggle(UUID id) {
        Subtask subtask = findOrThrow(id);
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean isOwnAssignee = subtask.getAssignee() != null && subtask.getAssignee().getId().equals(principal.id());
        boolean allowed = principal.role() == Role.ADMIN
                || (principal.role() == Role.HEAD && principal.belongsTo(subtask.getTask().getDepartment().getId()))
                || (principal.role() == Role.EMPLOYEE && isOwnAssignee);
        if (!allowed) {
            throw new ForbiddenException("Нет прав отмечать выполнение этой подзадачи");
        }
        SubtaskResponse before = toResponse(subtask);
        subtask.setDone(!subtask.isDone());
        subtask = subtaskRepository.save(subtask);
        auditLogService.record("Subtask", subtask.getId(), "subtask.toggle", before, toResponse(subtask));
        return toResponse(subtask);
    }

    public void delete(UUID id) {
        Subtask subtask = findOrThrow(id);
        assertDecomposeAccess(subtask.getTask());
        auditLogService.record("Subtask", subtask.getId(), "subtask.delete", toResponse(subtask), null);
        subtaskRepository.delete(subtask);
    }

    private Subtask findOrThrow(UUID id) {
        return subtaskRepository.findById(id).orElseThrow(() -> new NotFoundException("Subtask", id));
    }

    private SubtaskResponse toResponse(Subtask subtask) {
        return new SubtaskResponse(
                subtask.getId(), subtask.getTask().getId(), subtask.getTitle(),
                subtask.getAssignee() != null ? subtask.getAssignee().getId() : null,
                subtask.getAssignee() != null ? subtask.getAssignee().getFullName() : null,
                subtask.getDeadline(), subtask.isDone()
        );
    }
}
