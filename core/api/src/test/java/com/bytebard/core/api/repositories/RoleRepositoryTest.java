package com.bytebard.core.api.repositories;

import com.bytebard.core.api.config.PersistenceJpaConfigTest;
import com.bytebard.core.api.models.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { PersistenceJpaConfigTest.class, RoleRepository.class })
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Transactional
    @Rollback
    @Test
    void findByName_returnsRole_whenRoleExists() {
        roleRepository.save(new Role(Role.ADMIN));
        Optional<Role> result = roleRepository.findByName(Role.ADMIN);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(Role.ADMIN);
    }

    @Test
    void findByName_returnsEmpty_whenRoleDoesNotExist() {
        roleRepository.save(new Role(Role.ADMIN));
        Optional<Role> result = roleRepository.findByName("NON_EXISTENT_ROLE");

        assertThat(result).isEmpty();
    }
}
