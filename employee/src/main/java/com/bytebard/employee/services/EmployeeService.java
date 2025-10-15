package com.bytebard.employee.services;

import com.bytebard.core.api.context.AuthContext;
import com.bytebard.core.api.mappers.UserMapper;
import com.bytebard.core.api.models.Role;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.repositories.DepartmentRepository;
import com.bytebard.core.api.repositories.RoleRepository;
import com.bytebard.core.api.repositories.UserRepository;
import com.bytebard.core.api.types.UserDTO;
import com.bytebard.core.api.validators.FieldValidator;
import com.bytebard.employee.types.MutateUserRequest;
import com.bytebard.core.messaging.producer.UserEventsProducer;
import com.bytebard.utils.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmployeeService {
    private final DepartmentRepository departmentRepository;
    @Value("${spring.auth.default-password}")
    private String defaultPassword;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthContext authContext;
    private final UserEventsProducer producer;

    public EmployeeService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuthContext authContext, DepartmentRepository departmentRepository, UserEventsProducer producer) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authContext = authContext;
        this.departmentRepository = departmentRepository;
        this.producer = producer;
    }

    private void validateCreateUserRequest(MutateUserRequest request) {
        if (!FieldValidator.isValidEmail(request.getEmail())) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }

        if (!StringUtils.hasText(request.getFirstName())) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "First name cannot be empty");
        } else if (request.getFirstName().length() > 50) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "First name cannot exceed 50 characters");
        }

        if (!StringUtils.hasText(request.getLastName())) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Last name cannot be empty");
        } else if (request.getLastName().length() > 50) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Last name cannot exceed 50 characters");
        }

        if (!StringUtils.hasText(request.getRole()) || !List.of(Role.EMPLOYEE, Role.ADMIN, Role.MANAGER).contains(request.getRole())) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid user type");
        }
    }

    public UserDTO create(MutateUserRequest request) {
        validateCreateUserRequest(request);
        var role = roleRepository.findByName(request.getRole());
        if (role.isEmpty()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid user type");
        }

        var exists = userRepository.existsByEmail(request.getEmail());
        if (exists) {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User(
                request.getFirstName(),
                request.getLastName(),
                passwordEncoder.encode(defaultPassword),
                request.getEmail(),
                Status.INACTIVE,
                DateUtils.now(),
                Set.of(role.get())
        );
        user = userRepository.save(user);
        producer.sendUserCreatedEvent(user.getId());
        return UserMapper.toUserDTO(user);
    }

    public UserDTO update(Long id, MutateUserRequest request) {
        validateCreateUserRequest(request);
        var role = roleRepository.findByName(request.getRole());
        if (role.isEmpty()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid user type");
        }

        User user = userRepository.findById(id).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found."));

        if (!request.getEmail().equals(user.getEmail())) {
            var exists = userRepository.existsByEmail(request.getEmail(), user.getId());
            if (exists) {
                throw new HttpClientErrorException(HttpStatus.CONFLICT, "Email already exists");
            }
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        user = userRepository.save(user);
        return UserMapper.toUserDTO(user);
    }

    public void delete(Long id) {
        var exists = userRepository.existsById(id);
        if (!exists) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteUserDepartments(id);
        userRepository.deleteUserRoles(id);
        userRepository.deleteById(id);
    }

    public UserDTO getById(Long id) {
        var currentUser = authContext.getCurrentUser();
        if (currentUser.isEmployee() && currentUser.getId() != id) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (currentUser.isManager()) {
            boolean hasAccess = departmentRepository.hasAccessToUser(id, currentUser.getId());
            if (!hasAccess) {
                throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        var user = userRepository.findById(id).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.toUserDTO(user);
    }

    public List<UserDTO> getAllUsers(Integer page, Integer size) {
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<User> users;
        var currentUser = authContext.getCurrentUser();
        if (currentUser.isManager()) {
            users = userRepository.findUsersInSameDepartmentsAsManager(currentUser.getId(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.stream().map(UserMapper::toUserDTO).collect(Collectors.toList());
    }
}
