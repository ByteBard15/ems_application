package com.bytebard.employee.services;

import com.bytebard.core.api.context.AuthContext;
import com.bytebard.core.api.models.Role;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.repositories.DepartmentRepository;
import com.bytebard.core.api.repositories.RoleRepository;
import com.bytebard.core.api.repositories.UserRepository;
import com.bytebard.employee.types.MutateUserRequest;
import com.bytebard.core.messaging.producer.UserEventsProducer;
import com.bytebard.utils.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AuthContext authContext;

    @Mock
    private UserEventsProducer producer;

    private PasswordEncoder passwordEncoder;

    private EmployeeService employeeService;
    private AutoCloseable mocksCloseable;

    @BeforeEach
    void setup() {
        mocksCloseable = MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder();
        employeeService = new EmployeeService(userRepository, roleRepository, passwordEncoder, authContext, departmentRepository, producer);
        ReflectionTestUtils.setField(employeeService, "defaultPassword", "DefaultPwd123!");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocksCloseable != null) {
            mocksCloseable.close();
        }
    }

    @Test
    void create_succeeds_andReturnsUserDTO() {
        var req = new MutateUserRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("test@email.com");
        req.setRole(Role.ADMIN);

        var role = new Role(Role.ADMIN);
        when(roleRepository.findByName(Role.ADMIN)).thenReturn(Optional.of(role));

        var saved = new User("John", "Doe", passwordEncoder.encode("DefaultPwd123!"), "test@email.com", Status.INACTIVE, DateUtils.now(), Set.of(role));
        saved.setId(42L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        var dto = employeeService.create(req);

        assertNotNull(dto);
        assertEquals(42L, dto.getId());
        assertEquals("test@email.com", dto.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_fails_whenEmailInvalid() {
        var req = new MutateUserRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("not-an-email");
        req.setRole(Role.ADMIN);

        var ex = assertThrows(HttpClientErrorException.class, () -> employeeService.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void update_succeeds_andReturnsUserDTO() {
        var req = new MutateUserRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setEmail("jane.doe@example.com");
        req.setRole(Role.EMPLOYEE);

        var role = new Role(Role.EMPLOYEE);
        when(roleRepository.findByName(Role.EMPLOYEE)).thenReturn(Optional.of(role));

        var existing = new User("Old", "Name", "pw", "old@example.com", Status.ACTIVE, DateUtils.now(), Set.of(role));
        existing.setId(11L);
        when(userRepository.findById(11L)).thenReturn(Optional.of(existing));

        var saved = new User("Jane", "Doe", "pw2", "jane.doe@example.com", Status.ACTIVE, DateUtils.now(), Set.of(role));
        saved.setId(11L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        var dto = employeeService.update(11L, req);

        assertNotNull(dto);
        assertEquals(11L, dto.getId());
        assertEquals("jane.doe@example.com", dto.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void update_throwsNotFound_whenUserMissing() {
        when(roleRepository.findByName(Role.EMPLOYEE)).thenReturn(Optional.of(new Role(Role.EMPLOYEE)));

        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        var req = new MutateUserRequest();
        req.setFirstName("X");
        req.setLastName("Y");
        req.setEmail("x@y.com");
        req.setRole(Role.EMPLOYEE);

        var ex = assertThrows(HttpClientErrorException.class, () -> employeeService.update(999L, req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void update_fails_whenRoleInvalid() {
        var req = new MutateUserRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setEmail("jane.doe@example.com");
        req.setRole("UNKNOWN_ROLE");

        when(roleRepository.findByName("UNKNOWN_ROLE")).thenReturn(Optional.empty());

        var ex = assertThrows(HttpClientErrorException.class, () -> employeeService.update(5L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Invalid user type", ex.getStatusText());
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_fails_whenEmailAlreadyExists() {
        var req = new MutateUserRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setEmail("new@example.com");
        req.setRole(Role.EMPLOYEE);

        var role = new Role(Role.EMPLOYEE);
        var existing = new User("Old", "Name", "pw", "old@example.com", Status.ACTIVE, DateUtils.now(), Set.of(role));
        existing.setId(20L);

        when(roleRepository.findByName(Role.EMPLOYEE)).thenReturn(Optional.of(role));
        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@example.com", 20L)).thenReturn(true);

        var ex = assertThrows(HttpClientErrorException.class, () -> employeeService.update(20L, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Email already exists", ex.getStatusText());
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_fails_whenEmailInvalid() {
        var req = new MutateUserRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setEmail("invalid-email");
        req.setRole(Role.EMPLOYEE);

        var ex = assertThrows(HttpClientErrorException.class, () -> employeeService.update(1L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void delete_succeeds_whenExists() {
        when(userRepository.existsById(5L)).thenReturn(true);
        doNothing().when(userRepository).deleteUserDepartments(5L);
        doNothing().when(userRepository).deleteUserRoles(5L);
        doNothing().when(userRepository).deleteById(5L);

        employeeService.delete(5L);

        verify(userRepository).deleteUserDepartments(5L);
        verify(userRepository).deleteUserRoles(5L);
        verify(userRepository).deleteById(5L);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(userRepository.existsById(99L)).thenReturn(false);

        var ex = assertThrows(HttpClientErrorException.class, () -> employeeService.delete(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getById_deniesAccess_whenCurrentUserIsEmployeeAndDiffers() {
        var current = mock(User.class);
        when(current.isEmployee()).thenReturn(true);
        when(current.getId()).thenReturn(2L);
        when(authContext.getCurrentUser()).thenReturn(current);

        var ex = assertThrows(HttpClientErrorException.class, () -> employeeService.getById(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getById_deniesAccess_whenManagerHasNoDeptAccess() {
        var current = mock(User.class);
        when(current.isEmployee()).thenReturn(false);
        when(current.isManager()).thenReturn(true);
        when(current.getId()).thenReturn(10L);
        when(authContext.getCurrentUser()).thenReturn(current);

        when(departmentRepository.hasAccessToUser(1L, 10L)).thenReturn(false);

        var ex = assertThrows(HttpClientErrorException.class, () -> employeeService.getById(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getById_returnsUserDTO_whenAllowed() {
        var current = mock(User.class);
        when(current.isEmployee()).thenReturn(false);
        when(current.isManager()).thenReturn(true);
        when(current.getId()).thenReturn(10L);
        when(authContext.getCurrentUser()).thenReturn(current);

        when(departmentRepository.hasAccessToUser(1L, 10L)).thenReturn(true);

        var user = new User("F", "L", "pw", "u@example.com", Status.ACTIVE, DateUtils.now(), Set.of());
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var dto = employeeService.getById(1L);
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("u@example.com", dto.getEmail());
    }

    @Test
    void getAllUsers_callsFindAll_whenNotManager() {
        var current = mock(User.class);
        when(current.isManager()).thenReturn(false);
        when(authContext.getCurrentUser()).thenReturn(current);

        var list = List.of(
                new User("A", "B", "pw", "a@ex.com", Status.ACTIVE, DateUtils.now(), Set.of()),
                new User("C", "D", "pw", "c@ex.com", Status.ACTIVE, DateUtils.now(), Set.of())
        );
        list.get(0).setId(1L);
        list.get(1).setId(2L);

        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(list));

        var dtos = employeeService.getAllUsers(0, 10);
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }

    @Test
    void getAllUsers_usesManagerQuery_whenManager() {
        var current = mock(User.class);
        when(current.isManager()).thenReturn(true);
        when(current.getId()).thenReturn(100L);
        when(authContext.getCurrentUser()).thenReturn(current);

        var list = List.of(
                new User("M1", "U", "pw", "m1@ex.com", Status.ACTIVE, DateUtils.now(), Set.of())
        );
        list.get(0).setId(200L);

        when(userRepository.findUsersInSameDepartmentsAsManager(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(list));

        var dtos = employeeService.getAllUsers(0, 10);
        assertNotNull(dtos);
        assertEquals(1, dtos.size());
    }
}
