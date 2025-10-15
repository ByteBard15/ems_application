package com.bytebard.core.api.repositories;

import com.bytebard.core.api.config.PersistenceJpaConfigTest;
import com.bytebard.core.api.models.Department;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.models.UserDepartment;
import com.bytebard.utils.DateUtils;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { PersistenceJpaConfigTest.class, UserRepository.class })
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryFindUsersTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    private User manager;
    private Department dept1;
    private Department dept2;

    @BeforeEach
    void setUp() {
        manager = new User("mgrFirst", "mgrLast", "pw", "mgr@email.com", Status.ACTIVE, DateUtils.now());
        em.persist(manager);

        dept1 = new Department(null, "Dept A");
        dept2 = new Department(null, "Dept B");
        em.persist(dept1);
        em.persist(dept2);

        em.flush();
        em.clear();
    }

    @Test
    @Transactional
    @Rollback
    void findUsersInSameDepartmentsAsManager_returnsUsersWithinSameDept_includingManager() {
        manager = em.find(User.class, manager.getId());
        dept1 = em.find(Department.class, dept1.getId());

        em.persist(new UserDepartment(manager.getId(), dept1.getId()));

        var userA = new User("uAFirst", "uALast", "pw", "a@ex.com", Status.ACTIVE, DateUtils.now());
        em.persist(userA);
        em.persist(new UserDepartment(userA.getId(), dept1.getId()));

        var userB = new User("uBFirst", "uBLast", "pw", "b@ex.com", Status.ACTIVE, DateUtils.now());
        em.persist(userB);
        em.persist(new UserDepartment(userB.getId(), dept2.getId()));

        em.flush();
        em.clear();

        var page = userRepository.findUsersInSameDepartmentsAsManager(manager.getId(), PageRequest.of(0, 10));
        List<Long> ids = page.getContent().stream().map(User::getId).toList();

        assertTrue(ids.contains(manager.getId()), "result should include the manager");
        assertTrue(ids.contains(userA.getId()), "result should include userA in same dept");
        assertFalse(ids.contains(userB.getId()), "result should NOT include userB from other dept");
    }

    @Test
    @Transactional
    @Rollback
    void findUsersInSameDepartmentsAsManager_respectsPagination_andTotalCount() {
        manager = em.find(User.class, manager.getId());
        dept1 = em.find(Department.class, dept1.getId());
        em.persist(new UserDepartment(manager.getId(), dept1.getId()));

        List<User> created = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> new User("u" + i, "last" + i, "pw", "u" + i + "@ex.com", Status.ACTIVE, DateUtils.now()))
                .peek(em::persist)
                .toList();
        em.flush();

        created.forEach(u -> em.persist(new UserDepartment(u.getId(), dept1.getId())));

        em.flush();
        em.clear();

        var page0 = userRepository.findUsersInSameDepartmentsAsManager(manager.getId(), PageRequest.of(0, 10));
        assertEquals(10, page0.getContent().size());
        assertEquals(16, page0.getTotalElements());
        assertEquals(2, page0.getTotalPages());

        var page1 = userRepository.findUsersInSameDepartmentsAsManager(manager.getId(), PageRequest.of(1, 10));
        assertEquals(6, page1.getContent().size());
    }

    @Test
    @Transactional
    @Rollback
    void findUsersInSameDepartmentsAsManager_returnsUsersFromMultipleDepartments_forManagerWithMultipleMemberships() {
        manager = em.find(User.class, manager.getId());
        dept1 = em.find(Department.class, dept1.getId());
        dept2 = em.find(Department.class, dept2.getId());
        em.persist(new UserDepartment(manager.getId(), dept1.getId()));
        em.persist(new UserDepartment(manager.getId(), dept2.getId()));

        var user1 = new User("in1", "last", "pw", "in1@ex.com", Status.ACTIVE, DateUtils.now());
        em.persist(user1);
        em.persist(new UserDepartment(user1.getId(), dept1.getId()));

        var user2 = new User("in2", "last", "pw", "in2@ex.com", Status.ACTIVE, DateUtils.now());
        em.persist(user2);
        em.persist(new UserDepartment(user2.getId(), dept2.getId()));

        var userOut = new User("out", "last", "pw", "out@ex.com", Status.ACTIVE, DateUtils.now());
        em.persist(userOut);

        em.flush();
        em.clear();

        var page = userRepository.findUsersInSameDepartmentsAsManager(manager.getId(), PageRequest.of(0, 10));
        List<Long> ids = page.getContent().stream().map(User::getId).toList();

        assertTrue(ids.contains(user1.getId()));
        assertTrue(ids.contains(user2.getId()));
        assertFalse(ids.contains(userOut.getId()));
    }

    @Test
    @Transactional
    @Rollback
    void findUsersInSameDepartmentsAsManager_returnsEmpty_whenManagerHasNoDepartments() {
        em.flush();
        em.clear();

        var page = userRepository.findUsersInSameDepartmentsAsManager(manager.getId(), PageRequest.of(0, 10));
        assertNotNull(page);
        assertEquals(0, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
    }
}
