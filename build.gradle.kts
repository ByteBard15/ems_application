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