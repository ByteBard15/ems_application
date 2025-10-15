package com.bytebard.core.api.repositories;

import com.bytebard.core.api.models.Role;
import com.bytebard.core.api.models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM User u JOIN u.roles r " +
            "WHERE u.id = :userId AND r.name = :roleName")
    boolean userHasRole(@Param("userId") Long userId, @Param("roleName") String roleName);

    @Query("SELECT r FROM User u JOIN u.roles r WHERE u.id = :userId")
    List<Role> findRolesByUserId(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM User u JOIN u.roles r " +
            "WHERE u.id = :userId AND r.name IN :roles")
    boolean userHasAnyRole(@Param("userId") Long userId, @Param("roles") List<String> roles);

    @Query(value = """
        SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
        FROM users u JOIN user_roles ur ON u.id = ur.user_id
        JOIN roles r ON ur.role_id = r.id
        WHERE u.email = :email
          AND r.name = :roleName
    """, nativeQuery = true)
    boolean existsByEmailAndRole(
            @Param("email") String email,
            @Param("roleName") String roleName
    );

    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.id <> :userId")
    boolean existsByEmail(@Param("email") String email, @Param("userId") Long excludeUserId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_roles WHERE user_id = :userId", nativeQuery = true)
    void deleteUserRoles(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(
            value = "INSERT INTO user_roles(user_id, role_id) VALUES (:userId, :roleId)",
            nativeQuery = true
    )
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_departments WHERE user_id = :userId", nativeQuery = true)
    void deleteUserDepartments(@Param("userId") Long userId);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);

    @Query(
            value = "SELECT DISTINCT u.* " +
                    "FROM users u " +
                    "LEFT JOIN user_departments ud ON u.id = ud.user_id " +
                    "LEFT JOIN departments d ON ud.department_id = d.id " +
                    "LEFT JOIN user_departments ud2 ON d.id = ud2.department_id " +
                    "WHERE ud2.user_id = :managerId",
            countQuery = "SELECT COUNT(DISTINCT u.id) " +
                    "FROM users u " +
                    "LEFT JOIN user_departments ud ON u.id = ud.user_id " +
                    "LEFT JOIN departments d ON ud.department_id = d.id " +
                    "LEFT JOIN user_departments ud2 ON d.id = ud2.department_id " +
                    "WHERE ud2.user_id = :managerId",
            nativeQuery = true
    )
    Page<User> findUsersInSameDepartmentsAsManager(@Param("managerId") Long managerId, Pageable pageable);
}
