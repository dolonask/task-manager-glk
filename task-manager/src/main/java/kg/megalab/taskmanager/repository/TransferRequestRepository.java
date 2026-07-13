package kg.megalab.taskmanager.repository;

import kg.megalab.taskmanager.domain.Task;
import kg.megalab.taskmanager.domain.TransferRequest;
import kg.megalab.taskmanager.domain.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, UUID>, JpaSpecificationExecutor<TransferRequest> {

    List<TransferRequest> findByTask(Task task);

    List<TransferRequest> findByTaskAndStatusIn(Task task, List<TransferStatus> statuses);

    List<TransferRequest> findByTaskId(UUID taskId);
}
