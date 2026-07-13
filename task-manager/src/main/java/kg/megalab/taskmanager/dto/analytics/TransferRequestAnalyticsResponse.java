package kg.megalab.taskmanager.dto.analytics;

import java.util.List;
import java.util.UUID;

public record TransferRequestAnalyticsResponse(
        long total,
        double approvedRate,
        double rejectedRate,
        double avgShiftDays,
        List<DepartmentTransferStat> topDepartments
) {
    public record DepartmentTransferStat(UUID departmentId, String departmentName, long transferCount) {
    }
}
