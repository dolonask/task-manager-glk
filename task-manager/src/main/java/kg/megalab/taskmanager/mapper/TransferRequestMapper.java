package kg.megalab.taskmanager.mapper;

import kg.megalab.taskmanager.domain.TransferRequest;
import kg.megalab.taskmanager.dto.transfer.TransferRequestResponse;

public final class TransferRequestMapper {

    private TransferRequestMapper() {
    }

    public static TransferRequestResponse toResponse(TransferRequest tr) {
        return new TransferRequestResponse(
                tr.getId(), tr.getTask().getId(), tr.getTask().getTitle(),
                tr.getTask().getDepartment().getId(), tr.getTask().getDepartment().getName(),
                tr.getInitiator().getId(), tr.getInitiator().getFullName(),
                tr.getApprover() != null ? tr.getApprover().getId() : null,
                tr.getApprover() != null ? tr.getApprover().getFullName() : null,
                tr.getCurrentDeadline(), tr.getProposedDeadline(), tr.getJustification(),
                tr.getStatus().getValue(), tr.getDecisionReason(),
                tr.getSubmittedAt(), tr.getDecidedAt(), tr.getAppliedAt(),
                tr.getAppliedBy() != null ? tr.getAppliedBy().getId() : null,
                tr.getAppliedBy() != null ? tr.getAppliedBy().getFullName() : null
        );
    }
}
