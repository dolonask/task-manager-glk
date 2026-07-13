package kg.megalab.taskmanager.security;

import kg.megalab.taskmanager.domain.Role;

import java.util.Set;
import java.util.UUID;

/**
 * Lightweight principal decoded straight from JWT claims — no DB hit needed to
 * authorize a request. Services that need the full User entity look it up by id.
 */
public record UserPrincipal(
        UUID id,
        String login,
        Role role,
        UUID departmentId,
        Set<UUID> curatedDepartmentIds
) {
    public boolean curates(UUID departmentId) {
        return departmentId != null && curatedDepartmentIds.contains(departmentId);
    }

    public boolean belongsTo(UUID departmentId) {
        return departmentId != null && departmentId.equals(this.departmentId);
    }
}
