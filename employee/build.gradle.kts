plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency)
}

dependencyManagement {
    imports {
        val cloudBOM = libs.spring.cloud.dependencies.get().toString()
        val bootBOM = libs.spring.boot.dependencies.get().toString()
        mavenBom(cloudBOM)
        mavenBom(bootBOM)
    }
}

dependencies {
    implementation(project(":core:api"))
    implementation(project(":core:messaging"))
    implementation(libs.spring.cloud.starter.config)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
}

tasks.test {
    useJUnitPlatform()
}
