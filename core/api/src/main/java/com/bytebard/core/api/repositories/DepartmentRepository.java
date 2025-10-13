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
}
