package kg.megalab.taskmanager.repository;

import kg.megalab.taskmanager.domain.Subtask;
import kg.megalab.taskmanager.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubtaskRepository extends JpaRepository<Subtask, UUID> {

    List<Subtask> findByTask(Task task);

    List<Subtask> findByTaskId(UUID taskId);
}
