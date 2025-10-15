package com.bytebard.core.api.repositories;

import com.bytebard.core.api.models.Department;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_departments WHERE department_id = :departmentId", nativeQuery = true)
    void deleteUserDepartments(@Param("departmentId") Long departmentId);

    @Query(value = """
        SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
        FROM user_departments ud1
        JOIN user_departments ud2 ON ud1.department_id = ud2.department_id
        WHERE ud1.user_id = :empId
          AND ud2.user_id = :managerId
          AND ud1.department_id = ud2.department_id
    """, nativeQuery = true)
    boolean hasAccessToUser(
            @Param("empId") Long empId,
            @Param("managerId") Long managerId
    );

    boolean existsByName(String name);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Department d WHERE d.name = :name AND d.id <> :deptId")
    boolean existsByName(@Param("name") String name, @Param("deptId") Long excludeDeptId);
}
