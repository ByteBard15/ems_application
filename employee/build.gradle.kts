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
    implementation(libs.spring.cloud.starter.config)
}
