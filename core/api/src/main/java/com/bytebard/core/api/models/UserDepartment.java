package com.bytebard.core.api.models;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_departments")
@IdClass(UserDepartment.UserDepartmentId.class)
public class UserDepartment {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    public UserDepartment() {}

    public UserDepartment(Long userId, Long departmentId) {
        this.userId = userId;
        this.departmentId = departmentId;
    }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoleId() { return departmentId; }

    public void setRoleId(Long departmentId) { this.departmentId = departmentId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDepartment)) return false;
        UserDepartment that = (UserDepartment) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(departmentId, that.departmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, departmentId);
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "userId=" + userId +
                ", departmentId=" + departmentId +
                '}';
    }

    public static class UserDepartmentId implements Serializable {
        private Long userId;
        private Long departmentId;

        public UserDepartmentId() {}

        public UserDepartmentId(Long userId, Long departmentId) {
            this.userId = userId;
            this.departmentId = departmentId;
        }

        public Long getUserId() { return userId; }
        public Long getDepartmentId() { return departmentId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserDepartmentId)) return false;
            UserDepartmentId that = (UserDepartmentId) o;
            return Objects.equals(userId, that.userId) &&
                    Objects.equals(departmentId, that.departmentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, departmentId);
        }
    }
}
