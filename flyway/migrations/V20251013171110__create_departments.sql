-- Migration: create department
-- Created at: 2025-10-13T17:11:10.843240005
-- Version: 20251013171110

-- Write your SQL migration statements below

CREATE TABLE departments
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)
);
