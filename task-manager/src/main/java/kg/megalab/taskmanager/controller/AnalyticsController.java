package kg.megalab.taskmanager.controller;

import kg.megalab.taskmanager.dto.analytics.AnalyticsSummaryResponse;
import kg.megalab.taskmanager.dto.analytics.BoardMemberAnalyticsResponse;
import kg.megalab.taskmanager.dto.analytics.DepartmentAnalyticsResponse;
import kg.megalab.taskmanager.dto.analytics.RegistryResponse;
import kg.megalab.taskmanager.dto.analytics.TransferRequestAnalyticsResponse;
import kg.megalab.taskmanager.service.AnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics/summary")
    public AnalyticsSummaryResponse summary(@RequestParam(required = false) UUID departmentId) {
        return analyticsService.summary(departmentId);
    }

    @GetMapping("/analytics/departments")
    public List<DepartmentAnalyticsResponse> departments() {
        return analyticsService.byDepartment();
    }

    @GetMapping("/analytics/board-members")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public List<BoardMemberAnalyticsResponse> boardMembers() {
        return analyticsService.byBoardMember();
    }

    @GetMapping("/analytics/transfer-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public TransferRequestAnalyticsResponse transferRequests() {
        return analyticsService.transferRequests();
    }

    @GetMapping("/analytics/registry")
    public RegistryResponse registry(@RequestParam(required = false) UUID departmentId) {
        return analyticsService.registry(departmentId);
    }

    /** XLSX/PDF export is not implemented in this pass — see CLAUDE.md. */
    @GetMapping("/export/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public void exportTasks() {
    }

    @GetMapping("/export/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public void exportAnalytics() {
    }
}
