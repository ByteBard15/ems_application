package com.bytebard.core.api.repositories;

import com.bytebard.core.api.config.PersistenceJpaConfigTest;
import com.bytebard.core.api.models.Role;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.models.UserRole;
import com.bytebard.utils.DateUtils;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { PersistenceJpaConfigTest.class, UserRepository.class })
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    private Role rAdmin;
    private User adminUser;

    @BeforeEach
    void setUp() {
        var password = UUID.randomUUID().toString().replace("-", "");
        rAdmin = new Role(Role.ADMIN);
        em.persist(rAdmin);

        adminUser = new User("aFirstname", "aLastname", password, "aUser@email.com", Status.ACTIVE, DateUtils.now());
        em.persist(adminUser);
        em.persist(new UserRole(adminUser.getId(), rAdmin.getId()));

        em.flush();
        em.clear();
    }

    @Test
    void userHasRole_returnsTrueForUserWithRole() {
        User u = userRepository.findByEmail(adminUser.getEmail()).orElseThrow();
        boolean has = userRepository.userHasRole(u.getId(), Role.ADMIN);
        assertTrue(has);
    }

    @Test
    void findRolesByUserId_returnsRoleList() {
        User u = userRepository.findByEmail(adminUser.getEmail()).orElseThrow();
        List<Role> roles = userRepository.findRolesByUserId(u.getId());
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertTrue(roles.stream().allMatch(r -> Role.ADMIN.equals(r.getName())));
    }

    @Test
    void findByEmail_returnsOptionalUser() {
        Optional<User> maybe = userRepository.findByEmail(adminUser.getEmail());
        assertTrue(maybe.isPresent());
        assertEquals(adminUser.getEmail(), maybe.get().getEmail());
    }

    @Test
    void userHasAnyRole_worksForMultipleRoles() {
        boolean any = userRepository.userHasAnyRole(adminUser.getId(), List.of(Role.ADMIN, Role.EMPLOYEE));
        assertTrue(any);
    }

    @Test
    @Transactional
    @Rollback
    void existsByEmailAndRole_nativeQuery_returnsTrue() {
        boolean exists = userRepository.existsByEmailAndRole(adminUser.getEmail(), Role.ADMIN);
        assertTrue(exists);
    }

    @Test
    @Transactional
    @Rollback
    void deleteAndInsertUserRole_modifyingQueriesWork() {
        User u = userRepository.findByEmail(adminUser.getEmail()).orElseThrow();
        userRepository.deleteUserRoles(u.getId());
        userRepository.insertUserRole(u.getId(), rAdmin.getId());

        em.flush();
        em.clear();

        boolean hasAdmin = userRepository.userHasRole(u.getId(), Role.ADMIN);
        assertTrue(hasAdmin);
    }

    @Test
    void findWithRolesById_eagerLoadsRoles() {
        User u = userRepository.findWithRolesById(adminUser.getId()).orElseThrow();
        assertNotNull(u.getRoles());
        assertTrue(u.getRoles().stream().anyMatch(r -> Role.ADMIN.equals(r.getName())));
    }
}
