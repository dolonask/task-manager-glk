package kg.megalab.taskmanager.dto.analytics;

public record AnalyticsSummaryResponse(
        long totalTasks,
        long doneTasks,
        long inProgressTasks,
        long overdueTasks,
        double completionRate,
        double onTimeRate
) {
}
