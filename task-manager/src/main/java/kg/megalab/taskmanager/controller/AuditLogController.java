package kg.megalab.taskmanager.controller;

import kg.megalab.taskmanager.dto.audit.AuditLogEntryResponse;
import kg.megalab.taskmanager.dto.common.PageResponse;
import kg.megalab.taskmanager.service.AuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-log")
@PreAuthorize("hasAnyRole('ADMIN', 'BOARD', 'HEAD')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public PageResponse<AuditLogEntryResponse> list(@RequestParam(required = false) String entityType,
                                                      @RequestParam(required = false) UUID entityId,
                                                      @RequestParam(required = false) UUID actorId,
                                                      @RequestParam(required = false) Instant from,
                                                      @RequestParam(required = false) Instant to,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), 100);
        var pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "ts"));
        return PageResponse.of(auditLogService.search(entityType, entityId, actorId, from, to, pageable));
    }
}
