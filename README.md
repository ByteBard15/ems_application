# Employee Management Platform (Spring Boot Multi-Module)

This project is a **Spring Boot multi-module system** composed of **five core services** and **two supporting services**, designed to simulate a real-world microservices architecture with distributed configuration, authentication, discovery, and routing.

It provides a robust environment for managing users, departments, and roles across an organization, with support for role-based access control, service discovery, configuration management, and secure inter-service communication.

---

## Architecture Overview

### Core Services

| Service               | Description                                                                                                              |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| **config-service**    | Centralized configuration service that loads properties from the shared configuration repository.                        |
| **discovery-service** | Eureka-based service registry that allows dynamic service discovery across the network.                                  |
| **auth-service**      | Handles authentication and authorization logic, including JWT token issuance and validation.                             |
| **employee-service**  | Core domain service managing employees, departments, and role assignments.                                               |
| **gateway-service**   | API gateway that routes external requests to appropriate downstream services. Exposes documentation and entry endpoints. |

### Supporting Services

| Service                   | Description                                                           |
|---------------------------|-----------------------------------------------------------------------|
| **Gogs (Git Repository)** | Hosts the shared configuration repository used by the config-service. |
| **RabbitMQ**              | Provides messaging support for user and system events.                |
| **Postgres DB**           | Provides persistence support for models.                              |

In total, **8 containers** are created and orchestrated via Docker Compose.

---

## Continuous Integration (CI)

The CI workflow runs the following stages:

1. **Test Stage:** Executes tests across all modules using Gradle.
2. **Deploy Stage:** Builds, provisions, and starts all containers in sequence.

The first container to start is the **Gogs Git repository**, which mimics a private configuration network accessible only to internal services.

---

## Bootstrapping the Shared Config Repository

The shared configuration repository is created using the following Gradle properties (default values shown):

```kotlin
val ciHost = "127.0.0.1"
val liveHost = "127.0.0.1"
val gitUsername = "emp_admin"
val gitPassword = "admin"
val gitRepo = "shared-config"
val gitHostPort = "3001"
val gitInnerPort = "3000"
val gitContainerName = "shared-config-db"
val gitImageName = "gogs/gogs"
val gitTag = "0.11.91"
```

You can override these defaults in the start script or by running this command with you preferred values:

```bash
./gradlew --include-build bootstrap :bootstrap:createGitConfig --info \
  -PuseEnv=true \
  -PciHost="127.0.0.1" \
  -PliveHost="172.17.0.1"
```

---

## Environment Configuration

After the Git container is initialized, a **`default.env`** file is created in the project root.
This file defines all runtime environment variables used by the services (databases, ports, RabbitMQ, JWT secrets, etc.).

> ⚠️ **Important:**
> Do not manually change database credentials or container parameters in `default.env` unless you remove all existing containers (not necessarily the Git repo container) and re-run the bootstrap script.
> Otherwise, service initialization or migrations may fail.

---

## Database Setup
This is not really necessary if you run the start script.

1. **Create Database**

   ```bash
   ./flyway/create
   ```

2. **Run Migrations**

   ```bash
   ./flyway/migrate
   ```

The scripts will create the database using the credentials in `default.env` and apply all schema migrations automatically.

---

## Building the Application
After database setup, Gradle builds all modules and packages them as executable shadow JARs:
Note: This command is not also necessary if the start script is ran.
```bash
./gradlew clean buildSelectedJars --info --stacktrace
```

You can disable full stack traces by removing the `--stacktrace` or `--info` flag.

---

## Docker Compose Deployment

Once JARs are built, the Docker Compose file orchestrates container creation in the following order:

1. **RabbitMQ**
2. **Config-Service**
3. **Discovery-Service**
4. **Auth-Service**
5. **Employee-Service**
6. **Gateway-Service**

Each service is configured to pull its configuration from the shared Gogs repository.

The **Gateway Service** exposes port **`8080`** by default for external access.

---

## Automation & CI/CD

Two GitHub Actions files are included in the project to automate:

* **Testing:** Runs unit tests across all modules on every push to the `master` branch.
* **Deployment Simulation:** Boots up the environment and validates configuration setup via Docker Compose.

Additionally, two shell scripts in the project root simplify local management:

| Script      | Description                                                                                                       |
| ----------- | ----------------------------------------------------------------------------------------------------------------- |
| **`start`** | Runs the entire process (builds, creates containers, runs migrations, and starts services).                       |
| **`stop`**  | Stops all running containers. When called with the argument `full`, it deletes all created Docker images as well. |

Example usage:

```bash
./start

./stop

./stop full
```

These scripts allow you to run or tear down the entire stack without executing each command manually.

---

## Accessing the Application

### API Gateway

```
http://localhost:8080
```

### Swagger UI (API Documentation)

```
http://localhost:8080/webjars/swagger-ui/index.html#/default/login
```

---

## Default Accounts & Authentication Flow

When the system starts, a **default admin user** is automatically created and activated.

### Roles & Permissions

| Role         | Permissions                                          |
| ------------ | ---------------------------------------------------- |
| **Admin**    | Manage users, departments, and system configuration. |
| **Manager**  | View and manage users within their department only.  |
| **Employee** | View only their personal details.                    |

### Default Credentials

These are defined in the `default.env` file:

```
DEFAULT_PASSWORD=<your_default_password>
```

Users are created with this default password and are required to **change it before login**.

> In production, this password would be randomly generated and sent via email.
> For simplicity, a static password is used in this setup.

---

## Typical Lifecycle Summary

1. Run tests for all modules.
2. Create and initialize the Gogs shared config container.
3. Generate `default.env`.
4. Create and migrate database via Flyway scripts.
5. Build JARs for all modules.
6. Start containers via Docker Compose or `./start`.
7. Access the application via the Gateway on port 8080.
8. Stop containers with `./stop` (or remove everything using `./stop full`).

---

## Useful Commands

| Command                                                          | Description                              |
| ---------------------------------------------------------------- | ---------------------------------------- |
| `./gradlew clean test`                                           | Run all module tests.                    |
| `./gradlew --include-build bootstrap :bootstrap:createGitConfig` | Bootstrap shared Git configuration repo. |
| `./flyway/create`                                                | Create database.                         |
| `./flyway/migrate`                                               | Run database migrations.                 |
| `docker-compose up -d`                                           | Start all containers.                    |
| `docker-compose down`                                            | Stop and remove containers.              |
| `docker-compose build`                                           | Rebuild all images.                      |
| `./start`                                                        | Run the full process automatically.      |
| `./stop`                                                         | Stop running containers.                 |
| `./stop full`                                                    | Stop and delete all created images.      |

---

## Tech Stack

* **Spring Boot (Multi-Module Architecture)**
* **Spring Cloud Config**
* **Spring Cloud Netflix Eureka (Discovery)**
* **Spring Security (JWT)**
* **RabbitMQ**
* **PostgreSQL + Flyway**
* **Docker & Docker Compose**
* **Gradle Build System**
* **Swagger UI (OpenAPI Documentation)**
* **GitHub Actions CI**

---

## License

This project is provided for educational and demonstration purposes.
You’re free to modify and extend it for your own use.

---
