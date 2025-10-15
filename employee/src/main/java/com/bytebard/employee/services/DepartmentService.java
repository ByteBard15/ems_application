package com.bytebard.employee.services;

import com.bytebard.core.api.models.Department;
import com.bytebard.core.api.repositories.DepartmentRepository;
import com.bytebard.employee.types.MutateDepartmentRequest;
import com.bytebard.employee.types.DepartmentDTO;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public DepartmentDTO get(Long id) {
        var department = departmentRepository.findById(id).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Department not found"));
        return new DepartmentDTO(department.getId(), department.getName());
    }

    public List<DepartmentDTO> getAll(Integer page, Integer size) {
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Department> departmentsPage = departmentRepository.findAll(pageable);

        return departmentsPage.getContent().stream()
                .map(department -> new DepartmentDTO(department.getId(), department.getName()))
                .collect(Collectors.toList());
    }

    public DepartmentDTO create(MutateDepartmentRequest request) {
        if (!StringUtils.hasText(request.getName()) || request.getName().length() < 3) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Department name must be at least 3 characters");
        }

        var exists = departmentRepository.existsByName(request.getName());
        if (exists) {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, "Department name already exists");
        }

        var department = new Department(null, request.getName());
        department = departmentRepository.save(department);
        return new DepartmentDTO(department.getId(), department.getName());
    }

    public DepartmentDTO update(Long id, MutateDepartmentRequest request) {
        if (!StringUtils.hasText(request.getName()) || request.getName().length() < 3) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Department name must be at least 3 characters");
        }

        var exists = departmentRepository.existsByName(request.getName(), id);
        if (exists) {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, "Department name already exists");
        }

        var department = departmentRepository.findById(id).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Department not found"));
        department.setName(request.getName());
        department = departmentRepository.save(department);
        return new DepartmentDTO(department.getId(), department.getName());
    }

    @Transactional
    public void delete(Long id) {
        var exists = departmentRepository.existsById(id);
        if (!exists) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Department not found");
        }
        departmentRepository.deleteDepartmentUsers(id);
        departmentRepository.deleteById(id);
    }
}
