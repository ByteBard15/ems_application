subprojects {
    pluginManager.apply("org.gradle.java")
    pluginManager.apply("java-library")

    repositories {
        mavenCentral()
    }

    the<JavaPluginExtension>().apply {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

tasks.register("buildSelectedJars") {
    val selectedProjects = listOf(
        "auth",
        "config",
        "discovery",
        "gateway",
        "employee"
    )

    selectedProjects.forEach { projectName ->
        val proj = project(":$projectName")
        dependsOn(proj.tasks.named("bootJar"))
    }
}