package kg.megalab.taskmanager.dto.auth;

import java.util.List;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String fullName,
        String login,
        String role,
        UUID departmentId,
        List<UUID> curatedDepartmentIds
) {
}
