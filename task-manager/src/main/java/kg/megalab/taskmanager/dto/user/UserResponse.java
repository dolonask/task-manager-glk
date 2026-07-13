package kg.megalab.taskmanager.dto.user;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String login,
        String role,
        UUID departmentId,
        List<UUID> curatedDepartmentIds,
        boolean isActive
) {
}
