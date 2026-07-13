package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.domain.Department;
import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.Task;
import kg.megalab.taskmanager.domain.TaskStatus;
import kg.megalab.taskmanager.domain.TransferRequest;
import kg.megalab.taskmanager.domain.TransferStatus;
import kg.megalab.taskmanager.domain.User;
import kg.megalab.taskmanager.dto.analytics.AnalyticsSummaryResponse;
import kg.megalab.taskmanager.dto.analytics.BoardMemberAnalyticsResponse;
import kg.megalab.taskmanager.dto.analytics.DepartmentAnalyticsResponse;
import kg.megalab.taskmanager.dto.analytics.RegistryResponse;
import kg.megalab.taskmanager.dto.analytics.TransferRequestAnalyticsResponse;
import kg.megalab.taskmanager.repository.DepartmentRepository;
import kg.megalab.taskmanager.repository.TaskRepository;
import kg.megalab.taskmanager.repository.TransferRequestRepository;
import kg.megalab.taskmanager.repository.UserRepository;
import kg.megalab.taskmanager.security.SecurityUtils;
import kg.megalab.taskmanager.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final TaskService taskService;

    public AnalyticsService(TaskRepository taskRepository, DepartmentRepository departmentRepository,
                             UserRepository userRepository, TransferRequestRepository transferRequestRepository,
                             TaskService taskService) {
        this.taskRepository = taskRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.taskService = taskService;
    }

    /** Tasks visible to the current principal, per the same scoping rule as TaskService. */
    private List<Task> visibleTasks(UUID departmentId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        List<Task> all = taskRepository.findAll();
        return all.stream()
                .filter(t -> departmentId == null || t.getDepartment().getId().equals(departmentId))
                .filter(t -> switch (principal.role()) {
                    case ADMIN, BOARD, OBSERVER -> true;
                    case HEAD, EMPLOYEE -> principal.belongsTo(t.getDepartment().getId());
                })
                .toList();
    }

    public AnalyticsSummaryResponse summary(UUID departmentId) {
        List<Task> tasks = visibleTasks(departmentId);
        long total = tasks.size();
        long done = tasks.stream().filter(t -> TaskService.computeStatus(t) == TaskStatus.DONE).count();
        long inProgress = tasks.stream().filter(t -> TaskService.computeStatus(t) == TaskStatus.IN_PROGRESS).count();
        long overdue = tasks.stream().filter(t -> TaskService.computeStatus(t) == TaskStatus.OVERDUE).count();
        double completionRate = total == 0 ? 0 : (double) done / total;
        long doneOnTime = tasks.stream()
                .filter(t -> TaskService.computeStatus(t) == TaskStatus.DONE)
                .filter(t -> t.getClosedAt() == null
                        || !t.getClosedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate().isAfter(t.getCurrentDeadline()))
                .count();
        double onTimeRate = done == 0 ? 0 : (double) doneOnTime / done;
        return new AnalyticsSummaryResponse(total, done, inProgress, overdue, completionRate, onTimeRate);
    }

    public List<DepartmentAnalyticsResponse> byDepartment() {
        UserPrincipal principal = SecurityUtils.currentUser();
        List<Department> departments = departmentRepository.findAll().stream()
                .filter(d -> switch (principal.role()) {
                    case ADMIN, BOARD, OBSERVER -> true;
                    case HEAD, EMPLOYEE -> principal.belongsTo(d.getId());
                })
                .toList();
        return departments.stream().map(d -> {
            List<Task> tasks = visibleTasks(d.getId());
            long total = tasks.size();
            long done = tasks.stream().filter(t -> TaskService.computeStatus(t) == TaskStatus.DONE).count();
            long overdue = tasks.stream().filter(t -> TaskService.computeStatus(t) == TaskStatus.OVERDUE).count();
            double completionRate = total == 0 ? 0 : (double) done / total;
            return new DepartmentAnalyticsResponse(d.getId(), d.getName(), total, done, overdue, completionRate);
        }).toList();
    }

    public List<BoardMemberAnalyticsResponse> byBoardMember() {
        List<User> boardMembers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.BOARD)
                .toList();
        List<Task> allTasks = taskRepository.findAll();
        return boardMembers.stream().map(member -> {
            List<Task> created = allTasks.stream()
                    .filter(t -> t.getCreator().getId().equals(member.getId()))
                    .toList();
            Map<String, Long> breakdown = created.stream()
                    .collect(Collectors.groupingBy(t -> TaskService.computeStatus(t).getValue(), Collectors.counting()));
            return new BoardMemberAnalyticsResponse(member.getId(), member.getFullName(), created.size(), breakdown);
        }).toList();
    }

    public TransferRequestAnalyticsResponse transferRequests() {
        List<TransferRequest> all = transferRequestRepository.findAll();
        long total = all.size();
        long approved = all.stream().filter(t -> t.getStatus() == TransferStatus.APPROVED || t.getStatus() == TransferStatus.APPLIED).count();
        long rejected = all.stream().filter(t -> t.getStatus() == TransferStatus.REJECTED).count();
        double approvedRate = total == 0 ? 0 : (double) approved / total;
        double rejectedRate = total == 0 ? 0 : (double) rejected / total;
        double avgShiftDays = all.stream()
                .filter(t -> t.getStatus() == TransferStatus.APPLIED)
                .mapToLong(t -> ChronoUnit.DAYS.between(t.getCurrentDeadline(), t.getProposedDeadline()))
                .average().orElse(0);

        Map<UUID, Long> countByDepartment = all.stream()
                .collect(Collectors.groupingBy(t -> t.getTask().getDepartment().getId(), Collectors.counting()));
        List<TransferRequestAnalyticsResponse.DepartmentTransferStat> top = countByDepartment.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Department d = departmentRepository.findById(e.getKey()).orElse(null);
                    return new TransferRequestAnalyticsResponse.DepartmentTransferStat(
                            e.getKey(), d != null ? d.getName() : null, e.getValue());
                })
                .toList();

        return new TransferRequestAnalyticsResponse(total, approvedRate, rejectedRate, avgShiftDays, top);
    }

    public RegistryResponse registry(UUID departmentId) {
        List<Task> tasks = visibleTasks(departmentId);
        LocalDate horizon = LocalDate.now().plusDays(14);
        var upcoming = tasks.stream()
                .filter(t -> TaskService.computeStatus(t) != TaskStatus.DONE)
                .filter(t -> !t.getCurrentDeadline().isBefore(LocalDate.now()) && !t.getCurrentDeadline().isAfter(horizon))
                .sorted(Comparator.comparing(Task::getCurrentDeadline))
                .map(taskService::toListItem)
                .toList();
        var overdue = tasks.stream()
                .filter(t -> TaskService.computeStatus(t) == TaskStatus.OVERDUE)
                .sorted(Comparator.comparing(Task::getCurrentDeadline))
                .map(taskService::toListItem)
                .toList();
        return new RegistryResponse(upcoming, overdue);
    }
}
