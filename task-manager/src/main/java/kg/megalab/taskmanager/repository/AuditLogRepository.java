package kg.megalab.taskmanager.repository;

import kg.megalab.taskmanager.domain.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Deliberately exposes no update/delete beyond what JpaRepository inherits;
 * services must never call save() with an existing id or deleteById() for this entity.
 */
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID>, JpaSpecificationExecutor<AuditLogEntry> {
}
