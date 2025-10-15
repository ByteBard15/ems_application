package com.bytebard.employee.services;

import com.bytebard.core.api.models.Department;
import com.bytebard.core.api.repositories.DepartmentRepository;
import com.bytebard.employee.types.MutateDepartmentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private DepartmentService departmentService;
    private AutoCloseable mocksCloseable;

    @BeforeEach
    void setup() {
        mocksCloseable = MockitoAnnotations.openMocks(this);
        departmentService = new DepartmentService(departmentRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocksCloseable != null) {
            mocksCloseable.close();
        }
    }

    @Test
    void get_returnsDepartmentDTO_whenFound() {
        var dept = new Department(1L, "HR");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

        var dto = departmentService.get(1L);

        assertEquals(1L, dto.getId());
        assertEquals("HR", dto.getName());
    }

    @Test
    void get_throwsNotFound_whenMissing() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        var ex = assertThrows(HttpClientErrorException.class, () -> departmentService.get(1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getAll_returnsPagedDepartmentDTOs() {
        var list = List.of(new Department(1L, "HR"), new Department(2L, "Engineering"));
        when(departmentRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(list));

        var dtos = departmentService.getAll(null, null);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("HR", dtos.get(0).getName());
        assertEquals("Engineering", dtos.get(1).getName());
    }

    @Test
    void create_succeeds_andReturnsDTO() {
        var req = new MutateDepartmentRequest("Finance");
        var saved = new Department(10L, "Finance");
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        var dto = departmentService.create(req);

        assertEquals(10L, dto.getId());
        assertEquals("Finance", dto.getName());
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void create_fails_whenNameInvalid() {
        var req = new MutateDepartmentRequest("ab");

        var ex = assertThrows(HttpClientErrorException.class, () -> departmentService.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void update_succeeds_andReturnsUpdatedDTO() {
        var existing = new Department(5L, "OldName");
        when(departmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        var updated = new Department(5L, "NewName");
        when(departmentRepository.save(any(Department.class))).thenReturn(updated);

        var req = new MutateDepartmentRequest("NewName");
        var dto = departmentService.update(5L, req);

        assertEquals(5L, dto.getId());
        assertEquals("NewName", dto.getName());
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void update_fails_whenNotFound() {
        when(departmentRepository.findById(100L)).thenReturn(Optional.empty());
        var req = new MutateDepartmentRequest("Whatever");

        var ex = assertThrows(HttpClientErrorException.class, () -> departmentService.update(100L, req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void create_fails_whenNameAlreadyExists() {
        var req = new MutateDepartmentRequest("Finance");
        when(departmentRepository.existsByName("Finance")).thenReturn(true);

        var ex = assertThrows(HttpClientErrorException.class, () -> departmentService.create(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Department name already exists", ex.getStatusText());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_fails_whenNameInvalid() {
        var req = new MutateDepartmentRequest("ab");

        var ex = assertThrows(HttpClientErrorException.class, () -> departmentService.update(5L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Department name must be at least 3 characters", ex.getStatusText());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_fails_whenNameAlreadyExists() {
        var req = new MutateDepartmentRequest("Finance");
        when(departmentRepository.existsByName("Finance", 5L)).thenReturn(true);
        when(departmentRepository.findById(5L)).thenReturn(Optional.of(new Department(5L, "OldDept")));

        var ex = assertThrows(HttpClientErrorException.class, () -> departmentService.update(5L, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Department name already exists", ex.getStatusText());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void delete_succeeds_whenExists() {
        when(departmentRepository.existsById(3L)).thenReturn(true);
        doNothing().when(departmentRepository).deleteUserDepartments(3L);
        doNothing().when(departmentRepository).deleteById(3L);

        departmentService.delete(3L);

        verify(departmentRepository).deleteUserDepartments(3L);
        verify(departmentRepository).deleteById(3L);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(departmentRepository.existsById(99L)).thenReturn(false);

        var ex = assertThrows(HttpClientErrorException.class, () -> departmentService.delete(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
