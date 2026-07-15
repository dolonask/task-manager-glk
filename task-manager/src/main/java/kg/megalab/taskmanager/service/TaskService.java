package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.domain.Department;
import kg.megalab.taskmanager.domain.Priority;
import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.Subtask;
import kg.megalab.taskmanager.domain.Task;
import kg.megalab.taskmanager.domain.TaskStatus;
import kg.megalab.taskmanager.domain.User;
import kg.megalab.taskmanager.dto.subtask.SubtaskResponse;
import kg.megalab.taskmanager.dto.task.CreateTaskRequest;
import kg.megalab.taskmanager.dto.task.TaskDetailResponse;
import kg.megalab.taskmanager.dto.task.TaskListItemResponse;
import kg.megalab.taskmanager.dto.task.UpdateTaskRequest;
import kg.megalab.taskmanager.dto.transfer.TransferRequestResponse;
import kg.megalab.taskmanager.exception.ConflictException;
import kg.megalab.taskmanager.exception.ForbiddenException;
import kg.megalab.taskmanager.exception.InvalidStateTransitionException;
import kg.megalab.taskmanager.exception.NotFoundException;
import kg.megalab.taskmanager.repository.DepartmentRepository;
import kg.megalab.taskmanager.repository.TaskRepository;
import kg.megalab.taskmanager.repository.TransferRequestRepository;
import kg.megalab.taskmanager.repository.UserRepository;
import kg.megalab.taskmanager.security.SecurityUtils;
import kg.megalab.taskmanager.security.UserPrincipal;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final AuditLogService auditLogService;

    public TaskService(TaskRepository taskRepository, DepartmentRepository departmentRepository,
                        UserRepository userRepository, TransferRequestRepository transferRequestRepository,
                        AuditLogService auditLogService) {
        this.taskRepository = taskRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.auditLogService = auditLogService;
    }

    // ---- status/progress computation (spec 6.2, always server-side, never persisted) ----

    public static TaskStatus computeStatus(Task task) {
        List<Subtask> subtasks = task.getSubtasks();
        boolean allSubtasksDone = !subtasks.isEmpty() && subtasks.stream().allMatch(Subtask::isDone);
        if (task.getClosedAt() != null || allSubtasksDone) {
            return TaskStatus.DONE;
        }
        if (task.getCurrentDeadline().isBefore(LocalDate.now())) {
            return TaskStatus.OVERDUE;
        }
        if (subtasks.stream().anyMatch(Subtask::isDone)) {
            return TaskStatus.IN_PROGRESS;
        }
        return TaskStatus.NEW;
    }

    public static double computeProgress(Task task) {
        List<Subtask> subtasks = task.getSubtasks();
        if (subtasks.isEmpty()) {
            return computeStatus(task) == TaskStatus.DONE ? 1.0 : 0.0;
        }
        long done = subtasks.stream().filter(Subtask::isDone).count();
        return (double) done / subtasks.size();
    }

    // ---- visibility ----

    private boolean canView(UserPrincipal principal, UUID departmentId) {
        return switch (principal.role()) {
            case ADMIN, BOARD, OBSERVER -> true;
            case HEAD, EMPLOYEE -> principal.belongsTo(departmentId);
        };
    }

    private void assertCanView(UserPrincipal principal, Task task) {
        if (!canView(principal, task.getDepartment().getId())) {
            throw new ForbiddenException("Нет доступа к задачам этого структурного подразделения");
        }
    }

    // ---- queries ----

    @Transactional(readOnly = true)
    public List<TaskListItemResponse> list(String status, UUID departmentId, UUID creatorId, String priority, String q) {
        UserPrincipal principal = SecurityUtils.currentUser();

        Specification<Task> spec = (root, query, cb) -> cb.conjunction();
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (creatorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("creator").get("id"), creatorId));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), Priority.fromValue(priority)));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), like));
        }
        if (principal.role() == Role.HEAD || principal.role() == Role.EMPLOYEE) {
            UUID own = principal.departmentId();
            spec = spec.and((root, query, cb) -> own == null
                    ? cb.disjunction()
                    : cb.equal(root.get("department").get("id"), own));
        }

        List<TaskListItemResponse> items = taskRepository.findAll(spec).stream()
                .map(t -> new Object[]{t, computeStatus(t)})
                .filter(arr -> status == null || ((TaskStatus) arr[1]).getValue().equals(status))
                .map(arr -> toListItem((Task) arr[0]))
                .sorted(Comparator.comparing(TaskListItemResponse::currentDeadline))
                .toList();
        return items;
    }

    @Transactional(readOnly = true)
    public TaskDetailResponse getDetail(UUID id) {
        Task task = findOrThrow(id);
        assertCanView(SecurityUtils.currentUser(), task);
        return toDetail(task);
    }

    Task findOrThrow(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> new NotFoundException("Task", id));
    }

    // ---- mutations ----

    public TaskDetailResponse create(CreateTaskRequest request) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new NotFoundException("Department", request.departmentId()));

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDepartment(department);
        task.setCreator(userRepository.findById(SecurityUtils.currentUser().id())
                .orElseThrow(() -> new NotFoundException("User", SecurityUtils.currentUser().id())));
        task.setAssignee(request.assigneeId() != null
                ? userRepository.findById(request.assigneeId()).orElseThrow(() -> new NotFoundException("User", request.assigneeId()))
                : department.getHead());
        task.setPriority(Priority.fromValue(request.priority()));
        task.setInitialDeadline(request.initialDeadline());
        task.setCurrentDeadline(request.initialDeadline());

        task = taskRepository.save(task);
        auditLogService.record("Task", task.getId(), "task.create", null, toDetail(task));
        return toDetail(task);
    }

    public TaskDetailResponse update(UUID id, UpdateTaskRequest request) {
        if (request.currentDeadline() != null) {
            throw new InvalidStateTransitionException(
                    "Поле \"currentDeadline\" недоступно для прямого изменения — используйте регламент переноса срока");
        }
        Task task = findOrThrow(id);
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean isCreator = task.getCreator().getId().equals(principal.id());
        if (principal.role() != Role.ADMIN && !(principal.role() == Role.BOARD && isCreator)) {
            throw new ForbiddenException("Изменять задачу может администратор или поставившее её Правление");
        }

        TaskDetailResponse before = toDetail(task);
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.priority() != null) {
            task.setPriority(Priority.fromValue(request.priority()));
        }
        if (request.assigneeId() != null) {
            task.setAssignee(userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new NotFoundException("User", request.assigneeId())));
        }
        task = taskRepository.save(task);
        auditLogService.record("Task", task.getId(), "task.update", before, toDetail(task));
        return toDetail(task);
    }

    public void delete(UUID id) {
        Task task = findOrThrow(id);
        if (computeStatus(task) != TaskStatus.NEW) {
            throw new InvalidStateTransitionException("Удалить можно только задачу в статусе \"new\"");
        }
        List<kg.megalab.taskmanager.domain.TransferRequest> requests = transferRequestRepository.findByTask(task);
        boolean hasActiveRequest = requests.stream().anyMatch(tr -> tr.getStatus().isActive());
        if (hasActiveRequest) {
            throw new ConflictException("У задачи есть активная заявка на перенос срока");
        }
        auditLogService.record("Task", task.getId(), "task.delete", toDetail(task), null);
        // Terminal (rejected/applied) requests still FK-reference the task; the audit log above
        // already preserved their full history as an immutable JSON snapshot.
        transferRequestRepository.deleteAll(requests);
        taskRepository.delete(task);
    }

    public TaskDetailResponse close(UUID id) {
        Task task = findOrThrow(id);
        UserPrincipal principal = SecurityUtils.currentUser();
        // Spec §6.1 grants this to "admin, head курируемого СП" — interpreted as the head
        // of the task's own department, matching the department-scoping used elsewhere for `head`.
        boolean isOwnDepartmentHead = principal.role() == Role.HEAD && principal.belongsTo(task.getDepartment().getId());
        if (principal.role() != Role.ADMIN && !isOwnDepartmentHead) {
            throw new ForbiddenException("Закрыть задачу может администратор или начальник её СП");
        }
        TaskDetailResponse before = toDetail(task);
        task.setClosedAt(Instant.now());
        task = taskRepository.save(task);
        auditLogService.record("Task", task.getId(), "task.close", before, toDetail(task));
        return toDetail(task);
    }

    // ---- mapping ----

    TaskListItemResponse toListItem(Task task) {
        return new TaskListItemResponse(
                task.getId(), task.getTitle(), task.getDescription(),
                task.getCreator().getId(), task.getCreator().getFullName(),
                task.getDepartment().getId(), task.getDepartment().getName(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getFullName() : null,
                task.getPriority().getValue(), task.getInitialDeadline(), task.getCurrentDeadline(),
                computeStatus(task).getValue(), computeProgress(task),
                task.getCreatedAt(), task.getClosedAt()
        );
    }

    TaskDetailResponse toDetail(Task task) {
        List<SubtaskResponse> subtasks = task.getSubtasks().stream()
                .map(s -> new SubtaskResponse(s.getId(), task.getId(), s.getTitle(), s.getDescription(),
                        s.getAssignee() != null ? s.getAssignee().getId() : null,
                        s.getAssignee() != null ? s.getAssignee().getFullName() : null,
                        s.getDeadline(), s.isDone()))
                .toList();
        List<TransferRequestResponse> transferRequests = transferRequestRepository.findByTask(task).stream()
                .map(kg.megalab.taskmanager.mapper.TransferRequestMapper::toResponse)
                .toList();
        return new TaskDetailResponse(
                task.getId(), task.getTitle(), task.getDescription(),
                task.getCreator().getId(), task.getCreator().getFullName(),
                task.getDepartment().getId(), task.getDepartment().getName(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getFullName() : null,
                task.getPriority().getValue(), task.getInitialDeadline(), task.getCurrentDeadline(),
                computeStatus(task).getValue(), computeProgress(task),
                task.getCreatedAt(), task.getClosedAt(),
                subtasks, transferRequests
        );
    }

}
