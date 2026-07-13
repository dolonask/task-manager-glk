package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.domain.AuditLogEntry;
import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.User;
import kg.megalab.taskmanager.dto.audit.AuditLogEntryResponse;
import kg.megalab.taskmanager.repository.AuditLogRepository;
import kg.megalab.taskmanager.repository.SubtaskRepository;
import kg.megalab.taskmanager.repository.TaskRepository;
import kg.megalab.taskmanager.repository.TransferRequestRepository;
import kg.megalab.taskmanager.repository.UserRepository;
import kg.megalab.taskmanager.security.SecurityUtils;
import kg.megalab.taskmanager.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository,
                            TaskRepository taskRepository, SubtaskRepository subtaskRepository,
                            TransferRequestRepository transferRequestRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.subtaskRepository = subtaskRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.objectMapper = objectMapper;
    }

    /** Records an append-only entry. Never call save() again on the returned/persisted row. */
    public void record(String entityType, UUID entityId, String action, Object before, Object after) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setActor(userRepository.findById(SecurityUtils.currentUser().id()).orElse(null));
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setBeforeJson(writeJson(before));
        entry.setAfterJson(writeJson(after));
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntryResponse> search(String entityType, UUID entityId, UUID actorId,
                                               Instant from, Instant to, Pageable pageable) {
        Specification<AuditLogEntry> spec = (root, query, cb) -> cb.conjunction();
        if (entityType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (entityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }
        if (actorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actor").get("id"), actorId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ts"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("ts"), to));
        }

        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.role() == Role.HEAD) {
            List<UUID> visibleIds = visibleEntityIdsForHead(principal.departmentId());
            spec = spec.and((root, query, cb) -> root.get("entityId").in(visibleIds));
        }

        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /** Task/Subtask/TransferRequest ids belonging to the head's own department — the "своих задач" scope. */
    private List<UUID> visibleEntityIdsForHead(UUID departmentId) {
        if (departmentId == null) {
            return List.of();
        }
        var tasks = taskRepository.findAll((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        var taskIds = tasks.stream().map(kg.megalab.taskmanager.domain.Task::getId).toList();
        var subtaskIds = tasks.stream().flatMap(t -> subtaskRepository.findByTask(t).stream())
                .map(kg.megalab.taskmanager.domain.Subtask::getId);
        var transferIds = tasks.stream().flatMap(t -> transferRequestRepository.findByTask(t).stream())
                .map(kg.megalab.taskmanager.domain.TransferRequest::getId);
        return Stream.of(taskIds.stream(), subtaskIds, transferIds).flatMap(s -> s).toList();
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Object readJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private AuditLogEntryResponse toResponse(AuditLogEntry entry) {
        return new AuditLogEntryResponse(
                entry.getId(),
                entry.getTs(),
                entry.getActor() != null ? entry.getActor().getId() : null,
                entry.getActor() != null ? entry.getActor().getFullName() : null,
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getAction(),
                readJson(entry.getBeforeJson()),
                readJson(entry.getAfterJson())
        );
    }
}
