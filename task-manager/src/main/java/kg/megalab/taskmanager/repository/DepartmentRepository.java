package kg.megalab.taskmanager.repository;

import kg.megalab.taskmanager.domain.Department;
import kg.megalab.taskmanager.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findByCurator(User curator);

    List<Department> findByCuratorId(UUID curatorId);
}
