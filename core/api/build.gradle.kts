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
    api(project(":core:utils"))
    api(libs.spring.boot.starter.jpa)
    runtimeOnly(libs.postgres.core)
    api(libs.spring.boot.starter.web)
    api(libs.spring.cloud.eureka.client)
    api(libs.spring.boot.starter.security)
    api(libs.jwt.core)
}
