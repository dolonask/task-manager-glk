package kg.megalab.taskmanager.dto.department;

import java.util.UUID;

public record DepartmentResponse(UUID id, String name, UUID curatorId, String curatorName, UUID headId, String headName) {
}
