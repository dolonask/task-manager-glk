package kg.megalab.taskmanager.dto.department;

import java.util.UUID;

public record UpdateDepartmentRequest(String name, UUID curatorId, UUID headId) {
}
