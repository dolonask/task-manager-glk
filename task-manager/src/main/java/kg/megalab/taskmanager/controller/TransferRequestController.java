package kg.megalab.taskmanager.controller;

import kg.megalab.taskmanager.dto.transfer.CreateTransferRequestRequest;
import kg.megalab.taskmanager.dto.transfer.DecisionRequest;
import kg.megalab.taskmanager.dto.transfer.TransferRequestResponse;
import kg.megalab.taskmanager.service.TransferRequestService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TransferRequestController {

    private final TransferRequestService transferRequestService;

    public TransferRequestController(TransferRequestService transferRequestService) {
        this.transferRequestService = transferRequestService;
    }

    @GetMapping("/transfer-requests")
    public List<TransferRequestResponse> list(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) UUID taskId,
                                               @RequestParam(required = false) UUID departmentId,
                                               @RequestParam(required = false) UUID initiatorId,
                                               @RequestParam(required = false) UUID approverId) {
        return transferRequestService.list(status, taskId, departmentId, initiatorId, approverId);
    }

    @GetMapping("/transfer-requests/{id}")
    public TransferRequestResponse get(@PathVariable UUID id) {
        return transferRequestService.get(id);
    }

    @PostMapping("/tasks/{taskId}/transfer-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD')")
    public TransferRequestResponse create(@PathVariable UUID taskId, @RequestBody CreateTransferRequestRequest request) {
        return transferRequestService.create(taskId, request);
    }

    @PatchMapping("/transfer-requests/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD')")
    public TransferRequestResponse submit(@PathVariable UUID id) {
        return transferRequestService.submit(id);
    }

    @PatchMapping("/transfer-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public TransferRequestResponse approve(@PathVariable UUID id) {
        return transferRequestService.approve(id);
    }

    @PatchMapping("/transfer-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public TransferRequestResponse reject(@PathVariable UUID id, @RequestBody DecisionRequest request) {
        return transferRequestService.reject(id, request);
    }

    @PatchMapping("/transfer-requests/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOARD')")
    public TransferRequestResponse returnForRevision(@PathVariable UUID id, @RequestBody DecisionRequest request) {
        return transferRequestService.returnForRevision(id, request);
    }

    @PatchMapping("/transfer-requests/{id}/apply")
    @PreAuthorize("hasRole('ADMIN')")
    public TransferRequestResponse apply(@PathVariable UUID id) {
        return transferRequestService.apply(id);
    }
}
