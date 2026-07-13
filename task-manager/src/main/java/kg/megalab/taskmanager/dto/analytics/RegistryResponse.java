package kg.megalab.taskmanager.dto.analytics;

import kg.megalab.taskmanager.dto.task.TaskListItemResponse;

import java.util.List;

public record RegistryResponse(List<TaskListItemResponse> upcoming, List<TaskListItemResponse> overdue) {
}
