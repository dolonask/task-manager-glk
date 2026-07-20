package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.domain.Department;
import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.User;
import kg.megalab.taskmanager.dto.department.CreateDepartmentRequest;
import kg.megalab.taskmanager.dto.department.DepartmentResponse;
import kg.megalab.taskmanager.dto.department.UpdateDepartmentRequest;
import kg.megalab.taskmanager.exception.NotFoundException;
import kg.megalab.taskmanager.exception.ValidationException;
import kg.megalab.taskmanager.repository.DepartmentRepository;
import kg.megalab.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public DepartmentService(DepartmentRepository departmentRepository, UserRepository userRepository,
                              AuditLogService auditLogService) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public DepartmentResponse create(CreateDepartmentRequest request) {
        Department department = new Department();
        department.setName(request.name());
        department.setCurator(resolveCurator(request.curatorId()));
        department.setHead(resolveUser(request.headId()));
        department = departmentRepository.save(department);
        auditLogService.record("Department", department.getId(), "department.create", null, toResponse(department));
        return toResponse(department);
    }

    public DepartmentResponse update(UUID id, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department", id));
        DepartmentResponse before = toResponse(department);
        if (request.name() != null) {
            department.setName(request.name());
        }
        if (request.curatorId() != null) {
            department.setCurator(resolveCurator(request.curatorId()));
        }
        if (request.headId() != null) {
            department.setHead(resolveUser(request.headId()));
        }
        department = departmentRepository.save(department);
        auditLogService.record("Department", department.getId(), "department.update", before, toResponse(department));
        return toResponse(department);
    }

    Department findOrThrow(UUID id) {
        return departmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Department", id));
    }

    private User resolveUser(UUID id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    private User resolveCurator(UUID id) {
        User curator = resolveUser(id);
        if (curator != null && curator.getRole() != Role.BOARD) {
            throw new ValidationException("Курирующим может быть назначен только член правления", "curatorId");
        }
        return curator;
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCurator() != null ? department.getCurator().getId() : null,
                department.getCurator() != null ? department.getCurator().getFullName() : null,
                department.getHead() != null ? department.getHead().getId() : null,
                department.getHead() != null ? department.getHead().getFullName() : null
        );
    }
}
