subprojects {
    pluginManager.apply("org.gradle.java")
    pluginManager.apply("java-library")

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }

    the<JavaPluginExtension>().apply {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}