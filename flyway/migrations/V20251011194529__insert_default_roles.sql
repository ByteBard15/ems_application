-- Migration: Insert default roles
-- Created at: 2025-10-11T19:45:29.472848586
-- Version: 20251011194529

-- Write your SQL migration statements below

INSERT INTO users (name)
VALUES ('ADMIN'),
       ('USER'),
       ('MANAGER');
