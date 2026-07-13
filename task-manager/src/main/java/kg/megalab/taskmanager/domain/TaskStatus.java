package kg.megalab.taskmanager.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Always server-computed (see TaskService#computeStatus) — never persisted directly.
 */
public enum TaskStatus {
    NEW("new"),
    IN_PROGRESS("in_progress"),
    DONE("done"),
    OVERDUE("overdue");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
