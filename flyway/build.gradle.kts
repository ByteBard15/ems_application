import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.flyway.core)
    base
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

val propsFile = rootProject.file("default.env")

tasks.register("flywayMigrateBuildscript") {
    group = "flyway"
    description = "Run Flyway migrations using JARs on the buildscript classpath (flyway-core, db module, jdbc driver)"

    doLast {
        val props = Properties().apply {
            if (propsFile.exists()) {
                load(propsFile.inputStream())
            } else {
                throw GradleException("Missing default.properties file")
            }
        }

        val dbHost = props.getProperty("DB_HOST") ?: throw IllegalStateException("DB_HOST is required but not provided")

        val dbPort = props.getProperty("DB_PORT") ?: throw IllegalStateException("DB_PORT is required but not provided")

        val dbName = props.getProperty("DB_NAME") ?: throw IllegalStateException("DB_NAME is required but not provided")

        val user = props.getProperty("DB_USERNAME") ?: throw IllegalStateException("DB_USERNAME is required but not provided")

        val password = props.getProperty("spring.datasource.password")
            ?: System.getenv("DB_PASSWORD")
            ?: throw IllegalStateException("DB_PASSWORD is required but not provided")

        val locations = arrayOf("filesystem:${project.projectDir.path}/migrations")
        val url = "jdbc:postgresql://$dbHost:$dbPort/$dbName"

        val flyway = org.flywaydb.core.Flyway.configure()
            .dataSource(url, user, password)
            .locations(*locations)
            .load()

        val result = flyway.migrate()
        println("✅ Flyway completed. Migrations executed: ${result.migrationsExecuted}")
    }
}