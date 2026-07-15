package kg.megalab.taskmanager.controller;

import kg.megalab.taskmanager.dto.analytics.RegistryResponse;
import kg.megalab.taskmanager.dto.task.TaskListItemResponse;
import kg.megalab.taskmanager.exception.ValidationException;
import kg.megalab.taskmanager.service.AnalyticsService;
import kg.megalab.taskmanager.service.ExportService;
import kg.megalab.taskmanager.service.TaskService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private static final MediaType XLSX_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService exportService;
    private final TaskService taskService;
    private final AnalyticsService analyticsService;

    public ExportController(ExportService exportService, TaskService taskService, AnalyticsService analyticsService) {
        this.exportService = exportService;
        this.taskService = taskService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public ResponseEntity<byte[]> exportTasks(@RequestParam String format,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) UUID departmentId,
                                               @RequestParam(required = false) UUID creatorId,
                                               @RequestParam(required = false) String priority,
                                               @RequestParam(required = false) String q) {
        List<TaskListItemResponse> tasks = taskService.list(status, departmentId, creatorId, priority, q);
        return switch (normalizeFormat(format)) {
            case "xlsx" -> file(exportService.tasksXlsx(tasks), XLSX_TYPE, "tasks.xlsx");
            case "pdf" -> file(exportService.tasksPdf(tasks), MediaType.APPLICATION_PDF, "tasks.pdf");
            default -> throw unsupportedFormat();
        };
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public ResponseEntity<byte[]> exportAnalytics(@RequestParam String format,
                                                   @RequestParam(required = false) UUID departmentId) {
        var summary = analyticsService.summary(departmentId);
        var departments = analyticsService.byDepartment();
        var boardMembers = analyticsService.byBoardMember();
        var transferStats = analyticsService.transferRequests();
        RegistryResponse registry = analyticsService.registry(departmentId);

        return switch (normalizeFormat(format)) {
            case "xlsx" -> file(
                    exportService.analyticsXlsx(summary, departments, boardMembers, transferStats, registry),
                    XLSX_TYPE, "analytics.xlsx");
            case "pdf" -> file(
                    exportService.analyticsPdf(summary, departments, boardMembers, transferStats, registry),
                    MediaType.APPLICATION_PDF, "analytics.pdf");
            default -> throw unsupportedFormat();
        };
    }

    private static String normalizeFormat(String format) {
        return format == null ? "" : format.toLowerCase();
    }

    private static ValidationException unsupportedFormat() {
        return new ValidationException("Поле \"format\" должно быть \"xlsx\" или \"pdf\"", "format");
    }

    private static ResponseEntity<byte[]> file(byte[] body, MediaType type, String filename) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
