package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.Task;
import kg.megalab.taskmanager.domain.TransferRequest;
import kg.megalab.taskmanager.domain.TransferStatus;
import kg.megalab.taskmanager.dto.transfer.CreateTransferRequestRequest;
import kg.megalab.taskmanager.dto.transfer.DecisionRequest;
import kg.megalab.taskmanager.dto.transfer.TransferRequestResponse;
import kg.megalab.taskmanager.exception.ConflictException;
import kg.megalab.taskmanager.exception.ForbiddenException;
import kg.megalab.taskmanager.exception.InvalidStateTransitionException;
import kg.megalab.taskmanager.exception.NotFoundException;
import kg.megalab.taskmanager.exception.ValidationException;
import kg.megalab.taskmanager.mapper.TransferRequestMapper;
import kg.megalab.taskmanager.repository.TransferRequestRepository;
import kg.megalab.taskmanager.repository.UserRepository;
import kg.megalab.taskmanager.security.SecurityUtils;
import kg.megalab.taskmanager.security.UserPrincipal;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implements the three-stage deadline-transfer procedure from API spec §7:
 * justification (head, draft→pending) → approval (curating board member, pending→approved)
 * → application (admin only, approved→applied, writes task.currentDeadline).
 */
@Service
@Transactional
public class TransferRequestService {

    private final TransferRequestRepository transferRequestRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public TransferRequestService(TransferRequestRepository transferRequestRepository, UserRepository userRepository,
                                   TaskService taskService, AuditLogService auditLogService,
                                   NotificationService notificationService) {
        this.transferRequestRepository = transferRequestRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    private boolean canView(UserPrincipal principal, UUID departmentId) {
        return switch (principal.role()) {
            case ADMIN, BOARD, OBSERVER -> true;
            case HEAD, EMPLOYEE -> principal.belongsTo(departmentId);
        };
    }

    @Transactional(readOnly = true)
    public List<TransferRequestResponse> list(String status, UUID taskId, UUID departmentId,
                                               UUID initiatorId, UUID approverId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        Specification<TransferRequest> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), TransferStatus.fromValue(status)));
        }
        if (taskId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("task").get("id"), taskId));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("task").get("department").get("id"), departmentId));
        }
        if (initiatorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("initiator").get("id"), initiatorId));
        }
        if (approverId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("approver").get("id"), approverId));
        }
        return transferRequestRepository.findAll(spec).stream()
                .filter(tr -> canView(principal, tr.getTask().getDepartment().getId()))
                .map(TransferRequestMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransferRequestResponse get(UUID id) {
        TransferRequest tr = findOrThrow(id);
        if (!canView(SecurityUtils.currentUser(), tr.getTask().getDepartment().getId())) {
            throw new ForbiddenException("Нет доступа к заявкам этого структурного подразделения");
        }
        return TransferRequestMapper.toResponse(tr);
    }

    public TransferRequestResponse create(UUID taskId, CreateTransferRequestRequest request) {
        Task task = taskService.findOrThrow(taskId);
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean allowed = principal.role() == Role.ADMIN
                || (principal.role() == Role.HEAD && principal.belongsTo(task.getDepartment().getId()));
        if (!allowed) {
            throw new ForbiddenException("Заявку может создать только начальник СП-исполнителя или администратор");
        }

        boolean hasActive = transferRequestRepository.findByTaskAndStatusIn(task,
                        List.of(TransferStatus.DRAFT, TransferStatus.PENDING, TransferStatus.REVISION))
                .stream().findAny().isPresent();
        if (hasActive) {
            throw new ConflictException("По задаче уже есть активная заявка на перенос срока");
        }

        TransferRequest tr = new TransferRequest();
        tr.setTask(task);
        tr.setInitiator(userRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("User", principal.id())));
        tr.setApprover(task.getDepartment().getCurator());
        tr.setCurrentDeadline(task.getCurrentDeadline());
        tr.setProposedDeadline(request.proposedDeadline());
        tr.setJustification(request.justification());
        tr.setStatus(TransferStatus.DRAFT);

        tr = transferRequestRepository.save(tr);
        auditLogService.record("TransferRequest", tr.getId(), "transfer_request.create", null, TransferRequestMapper.toResponse(tr));
        return TransferRequestMapper.toResponse(tr);
    }

    public TransferRequestResponse submit(UUID id) {
        TransferRequest tr = findOrThrow(id);
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean allowed = principal.role() == Role.ADMIN || tr.getInitiator().getId().equals(principal.id());
        if (!allowed) {
            throw new ForbiddenException("Отправить заявку может только её инициатор или администратор");
        }
        if (tr.getStatus() != TransferStatus.DRAFT && tr.getStatus() != TransferStatus.REVISION) {
            throw new InvalidStateTransitionException("Отправить на согласование можно только заявку в статусе \"draft\" или \"revision\"");
        }
        if (tr.getProposedDeadline() == null) {
            throw new ValidationException("Поле \"proposedDeadline\" обязательно для заполнения", "proposedDeadline");
        }
        if (tr.getJustification() == null || tr.getJustification().isBlank()) {
            throw new ValidationException("Поле \"justification\" обязательно для заполнения", "justification");
        }

        TransferRequestResponse before = TransferRequestMapper.toResponse(tr);
        tr.setStatus(TransferStatus.PENDING);
        tr.setSubmittedAt(Instant.now());
        tr.setDecisionReason(null);
        tr = transferRequestRepository.save(tr);
        auditLogService.record("TransferRequest", tr.getId(), "transfer_request.submit", before, TransferRequestMapper.toResponse(tr));

        if (tr.getApprover() != null) {
            notificationService.notify(tr.getApprover(),
                    "Поступила заявка на перенос срока по задаче \"" + tr.getTask().getTitle() + "\" на согласование",
                    "TransferRequest", tr.getId());
        }
        return TransferRequestMapper.toResponse(tr);
    }

    private void assertApproverAccess(TransferRequest tr) {
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean allowed = principal.role() == Role.ADMIN
                || (principal.role() == Role.BOARD && principal.curates(tr.getTask().getDepartment().getId()));
        if (!allowed) {
            throw new ForbiddenException("Согласовать заявку может только курирующий член Правления или администратор");
        }
    }

    public TransferRequestResponse approve(UUID id) {
        TransferRequest tr = findOrThrow(id);
        assertApproverAccess(tr);
        if (tr.getStatus() != TransferStatus.PENDING) {
            throw new InvalidStateTransitionException("Согласовать можно только заявку в статусе \"pending\"");
        }
        TransferRequestResponse before = TransferRequestMapper.toResponse(tr);
        tr.setStatus(TransferStatus.APPROVED);
        tr.setDecidedAt(Instant.now());
        tr = transferRequestRepository.save(tr);
        auditLogService.record("TransferRequest", tr.getId(), "transfer_request.approve", before, TransferRequestMapper.toResponse(tr));

        notificationService.notify(tr.getInitiator(),
                "Заявка на перенос срока по задаче \"" + tr.getTask().getTitle() + "\" согласована",
                "TransferRequest", tr.getId());
        notifyAllAdmins("Заявка на перенос срока по задаче \"" + tr.getTask().getTitle() + "\" согласована и ждёт применения", tr.getId());
        return TransferRequestMapper.toResponse(tr);
    }

    public TransferRequestResponse reject(UUID id, DecisionRequest request) {
        return decide(id, TransferStatus.REJECTED, request, "заявка отклонена");
    }

    public TransferRequestResponse returnForRevision(UUID id, DecisionRequest request) {
        return decide(id, TransferStatus.REVISION, request, "заявка возвращена на доработку");
    }

    private TransferRequestResponse decide(UUID id, TransferStatus target, DecisionRequest request, String noticeSuffix) {
        TransferRequest tr = findOrThrow(id);
        assertApproverAccess(tr);
        if (tr.getStatus() != TransferStatus.PENDING) {
            throw new InvalidStateTransitionException("Решение можно принять только по заявке в статусе \"pending\"");
        }
        if (request.decisionReason() == null || request.decisionReason().isBlank()) {
            throw new ValidationException("Поле \"decisionReason\" обязательно для заполнения", "decisionReason");
        }
        TransferRequestResponse before = TransferRequestMapper.toResponse(tr);
        tr.setStatus(target);
        tr.setDecisionReason(request.decisionReason());
        tr.setDecidedAt(Instant.now());
        tr = transferRequestRepository.save(tr);
        String action = target == TransferStatus.REJECTED ? "transfer_request.reject" : "transfer_request.return";
        auditLogService.record("TransferRequest", tr.getId(), action, before, TransferRequestMapper.toResponse(tr));

        notificationService.notify(tr.getInitiator(),
                "По задаче \"" + tr.getTask().getTitle() + "\": " + noticeSuffix,
                "TransferRequest", tr.getId());
        return TransferRequestMapper.toResponse(tr);
    }

    public TransferRequestResponse apply(UUID id) {
        TransferRequest tr = findOrThrow(id);
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.role() != Role.ADMIN) {
            throw new ForbiddenException("Применить новый срок может только администратор");
        }
        if (tr.getStatus() != TransferStatus.APPROVED) {
            throw new InvalidStateTransitionException("Применить можно только заявку в статусе \"approved\"");
        }
        TransferRequestResponse before = TransferRequestMapper.toResponse(tr);

        Task task = tr.getTask();
        LocalDateHolder taskBefore = new LocalDateHolder(task.getCurrentDeadline());
        task.setCurrentDeadline(tr.getProposedDeadline());

        tr.setStatus(TransferStatus.APPLIED);
        tr.setAppliedAt(Instant.now());
        tr.setAppliedBy(userRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("User", principal.id())));
        tr = transferRequestRepository.save(tr);

        auditLogService.record("TransferRequest", tr.getId(), "transfer_request.apply", before, TransferRequestMapper.toResponse(tr));
        auditLogService.record("Task", task.getId(), "task.deadline_applied", taskBefore, task.getCurrentDeadline());

        notificationService.notify(task.getCreator(),
                "Применён новый срок по задаче \"" + task.getTitle() + "\": " + task.getCurrentDeadline(),
                "Task", task.getId());
        return TransferRequestMapper.toResponse(tr);
    }

    private void notifyAllAdmins(String message, UUID entityId) {
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .forEach(admin -> notificationService.notify(admin, message, "TransferRequest", entityId));
    }

    private TransferRequest findOrThrow(UUID id) {
        return transferRequestRepository.findById(id).orElseThrow(() -> new NotFoundException("TransferRequest", id));
    }

    /** Small record purely so the audit "before" snapshot serializes as {"currentDeadline": ...} rather than a bare string. */
    private record LocalDateHolder(java.time.LocalDate currentDeadline) {
    }
}
