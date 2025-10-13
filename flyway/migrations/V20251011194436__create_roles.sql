-- Migration: Create roles
-- Created at: 2025-10-11T19:44:36.383843743
-- Version: 20251011194436

-- Write your SQL migration statements below

CREATE TABLE roles
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE INDEX idx_roles_name ON roles (name);
