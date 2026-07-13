package kg.megalab.taskmanager.dto.task;

import java.util.UUID;

/**
 * Partial update. {@code currentDeadline} is intentionally accepted here only so the
 * service layer can detect its presence and reject it with 422 INVALID_STATE_TRANSITION —
 * per the spec, it may only change via the transfer-request "apply" step.
 */
public record UpdateTaskRequest(
        String title,
        String description,
        String priority,
        UUID assigneeId,
        Object currentDeadline
) {
}
