-- Migration: Create users
-- Created at: 2025-10-11T19:41:27.633209574
-- Version: 20251011194127

-- Write your SQL migration statements below

CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100),
    last_name  VARCHAR(100),
    password  VARCHAR(255),
    email      VARCHAR(150) UNIQUE NOT NULL,
    status     VARCHAR(50),
    created_at TIMESTAMP
);
