package com.bytebard.auth.runners;

import com.bytebard.core.api.models.Role;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.repositories.RoleRepository;
import com.bytebard.core.api.repositories.UserRepository;
import com.bytebard.utils.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InsertDefaultAdmin implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.auth.default-password:defaultPassword}")
    private String defaultPassword;

    public InsertDefaultAdmin(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public void run(String... args) {
        var exists = userRepository.existsByEmailAndRole("admin@emp.com", Role.ADMIN);
        if (exists) {
            return;
        }
        User user = new User(
                "admin", "admin", passwordEncoder.encode(defaultPassword), "admin@emp.com", Status.ACTIVE, DateUtils.now(), null
        );
        user = userRepository.save(user);
        var findRole = roleRepository.findByName(Role.ADMIN);
        Role adminRole;
        if (findRole.isEmpty()) {
            adminRole = new Role(Role.ADMIN);
            roleRepository.save(adminRole);
        } else {
            adminRole = findRole.get();
        }
        userRepository.insertUserRole(user.getId(), adminRole.getId());
    }
}
