-- Migration: Create user roles
-- Created at: 2025-10-11T19:44:57.279086812
-- Version: 20251011194457

-- Write your SQL migration statements below

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY (role_id)
        REFERENCES roles (id)
        ON DELETE CASCADE
);
