package kg.megalab.taskmanager.dto.transfer;

import java.time.LocalDate;

public record CreateTransferRequestRequest(LocalDate proposedDeadline, String justification) {
}
