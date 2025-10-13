-- Migration: create user departments
-- Created at: 2025-10-13T17:12:16.288670871
-- Version: 20251013171216

-- Write your SQL migration statements below

CREATE TABLE user_departments
(
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, department_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_departments FOREIGN KEY (department_id)
        REFERENCES departments (id)
        ON DELETE CASCADE
);
