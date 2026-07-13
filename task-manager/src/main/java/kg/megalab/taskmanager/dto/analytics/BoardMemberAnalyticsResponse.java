package kg.megalab.taskmanager.dto.analytics;

import java.util.Map;
import java.util.UUID;

public record BoardMemberAnalyticsResponse(
        UUID boardMemberId,
        String fullName,
        long totalTasks,
        Map<String, Long> statusBreakdown
) {
}
