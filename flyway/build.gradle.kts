import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.flyway.core)
}

buildscript {
    dependencies {
        classpath(libs.postgres.core)
        classpath(libs.flyway.database)
        classpath(libs.flyway.core)

    }
}

tasks.register("create-migration") {
    group = "flyway"
    description = "Creates a new Flyway migration SQL file with proper naming convention"

    val migrationDir = layout.projectDirectory.dir("migrations")
    val migrationName = project.findProperty("title") as? String ?: throw Exception("Provide a valid migration name")

    doLast {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val fileName = "V${timestamp}__${migrationName.replace(" ", "_").lowercase()}.sql"
        val file = migrationDir.file(fileName).asFile

        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }

        file.writeText(
            """
            -- Migration: $migrationName
            -- Created at: ${LocalDateTime.now()}
            -- Version: $timestamp

            -- Write your SQL migration statements below

            """.trimIndent()
        )

        println("✅ Created migration file: ${file.absolutePath}")
    }
}

tasks.register("flywayMigrateBuildscript") {
    group = "flyway"
    description = "Run Flyway migrations using JARs on the buildscript classpath (flyway-core, db module, jdbc driver)"

    doLast {
        val url = (project.findProperty("flyway.url") as? String)
            ?: System.getenv("FLYWAY_URL")
            ?: "jdbc:postgresql://localhost:5434/emp_db"

        val user = (project.findProperty("flyway.user") as? String)
            ?: System.getenv("FLYWAY_USER")
            ?: "emp_db"

        val password = (project.findProperty("flyway.password") as? String)
            ?: System.getenv("FLYWAY_PASSWORD")
            ?: "emp_db"

        val locations = arrayOf("filesystem:${project.projectDir.path}/migrations")

        println("Running Flyway migrate -> url=$url user=${user.ifBlank { "<empty>" }}")

        val flyway = org.flywaydb.core.Flyway.configure()
            .dataSource(url, user, password)
            .locations(*locations)
            .load()

        val result = flyway.migrate()
        println("✅ Flyway completed. Migrations executed: ${result.migrationsExecuted}")
    }
}