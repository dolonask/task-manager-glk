package kg.megalab.taskmanager.dto.analytics;

import java.util.UUID;

public record DepartmentAnalyticsResponse(
        UUID departmentId,
        String departmentName,
        long totalTasks,
        long doneTasks,
        long overdueTasks,
        double completionRate
) {
}
