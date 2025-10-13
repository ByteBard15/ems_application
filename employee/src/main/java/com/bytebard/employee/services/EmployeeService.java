package com.bytebard.employee.services;

import com.bytebard.core.api.context.AuthContext;
import com.bytebard.core.api.mappers.UserMapper;
import com.bytebard.core.api.models.Role;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.repositories.RoleRepository;
import com.bytebard.core.api.repositories.UserRepository;
import com.bytebard.core.api.types.UserDTO;
import com.bytebard.core.api.validators.FieldValidator;
import com.bytebard.employee.types.MutateUserRequest;
import com.bytebard.utils.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Set;

@Service
public class EmployeeService {
    @Value("${spring.auth.default-password}")
    private String defaultPassword;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthContext authContext;

    public EmployeeService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuthContext authContext) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authContext = authContext;
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

        if (List.of(Role.EMPLOYEE, Role.ADMIN, Role.MANAGER).contains(request.getUserType())) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid user type");
        }
    }

    public UserDTO create(MutateUserRequest request) {
        validateCreateUserRequest(request);
        var role = roleRepository.findByName(request.getUserType());
        if (role.isEmpty()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid user type");
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
        return UserMapper.toUserDTO(user);
    }

    public UserDTO update(Long id, MutateUserRequest request) {
        validateCreateUserRequest(request);
        var role = roleRepository.findByName(request.getUserType());
        if (role.isEmpty()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid user type");
        }

        User user = userRepository.findById(id).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found."));
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
            boolean hasAccess = userRepository.hasAccessToUser(id, currentUser.getId());
            if (!hasAccess) {
                throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        var user = userRepository.findById(id).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.toUserDTO(user);
    }
}
