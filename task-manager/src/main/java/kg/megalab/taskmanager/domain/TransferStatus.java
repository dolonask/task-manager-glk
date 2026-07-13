package kg.megalab.taskmanager.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransferStatus {
    DRAFT("draft"),
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    REVISION("revision"),
    APPLIED("applied");

    private final String value;

    TransferStatus(String value) {
        this.value = value;
    }

    public boolean isActive() {
        return this == DRAFT || this == PENDING || this == REVISION;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransferStatus fromValue(String value) {
        for (TransferStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown transfer status: " + value);
    }
}
