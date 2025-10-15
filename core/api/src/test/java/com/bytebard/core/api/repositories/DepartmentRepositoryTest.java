package com.bytebard.core.api.repositories;

import com.bytebard.core.api.config.PersistenceJpaConfigTest;
import com.bytebard.core.api.models.Department;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.models.UserDepartment;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { PersistenceJpaConfigTest.class, DepartmentRepository.class })
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TestEntityManager em;

    private User employee;
    private User manager;

    @BeforeEach
    void setUp() {
        employee = new User("eFirstname", "eLastname", UUID.randomUUID().toString(), "emp@email.com", Status.ACTIVE, DateUtils.now());
        manager = new User("mFirstname", "mLastname", UUID.randomUUID().toString(), "manager@email.com", Status.ACTIVE, DateUtils.now());
        em.persist(manager);
        em.persist(employee);
    }

    @Test
    @Transactional
    @Rollback
    void hasAccessToUser_returnsTrue_whenUsersShareDepartment() {
        var department = new Department("Engineering");
        em.persist(department);
        var eDept = new UserDepartment(employee.getId(), department.getId());
        var mDept = new UserDepartment(manager.getId(), department.getId());
        em.persist(eDept);
        em.persist(mDept);

        boolean result = departmentRepository.hasAccessToUser(employee.getId(), manager.getId());
        assertThat(result).isTrue();
    }

    @Test
    void hasAccessToUser_returnsFalse_whenUsersDoNotShareDepartment() {
        var yDept = new Department("Youtube");
        var tDept = new Department("Twitter");
        em.persist(yDept);
        em.persist(tDept);

        var eDept = new UserDepartment(employee.getId(), yDept.getId());
        var mDept = new UserDepartment(manager.getId(), tDept.getId());
        em.persist(eDept);
        em.persist(mDept);

        boolean result = departmentRepository.hasAccessToUser(employee.getId(), manager.getId());

        assertThat(result).isFalse();
    }

    @Transactional
    @Rollback
    @Test
    void deleteUserDepartments_removesAllUserLinksToDepartment() {
        var department = new Department("Engineering");
        em.persist(department);
        var eDept = new UserDepartment(employee.getId(), department.getId());
        var mDept = new UserDepartment(manager.getId(), department.getId());
        em.persist(eDept);
        em.persist(mDept);

        departmentRepository.deleteUserDepartments(department.getId());

        Long count = ((Number) em.getEntityManager().createNativeQuery(
                        "SELECT COUNT(*) FROM user_departments WHERE department_id = ?1")
                .setParameter(1, department.getId())
                .getSingleResult()).longValue();

        assertThat(count).isZero();
    }
}
