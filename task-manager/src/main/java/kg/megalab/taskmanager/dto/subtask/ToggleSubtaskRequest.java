package kg.megalab.taskmanager.dto.subtask;

/**
 * {@code comment} is required only when the toggle marks the subtask done (validated in
 * the service, since that depends on the subtask's current state) — omitted when un-marking.
 */
public record ToggleSubtaskRequest(String comment) {
}
