package kg.megalab.taskmanager.dto.user;

import java.util.UUID;

/** Partial update — null fields are left unchanged. departmentId cannot be cleared this way; pass a valid id or omit. */
public record UpdateUserRequest(String fullName, String login, String password, String role, UUID departmentId, Boolean isActive) {
}
